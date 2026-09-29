package com.example.randomGallery.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.randomGallery.config.EnvContext;
import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.service.mapper.GroupServiceMapper;
import com.example.randomGallery.service.mapper.PicServiceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 随机取图不再把 84 万条 ID 常驻内存：改为「缓存区间 + 主键索引起跳」，
 * 这里守住它的对外契约——一定返回有效 ID，且只在无有效图片时才返回 null。
 */
class CacheServiceTest {

    private static PicDO pic(Integer id, Long groupId) {
        PicDO pic = new PicDO();
        pic.setId(id);
        pic.setGroupId(groupId);
        return pic;
    }

    /**
     * cachePicId() 会按「图片/分组 × DESC/ASC」四次调用 selectOne，按 wrapper 的 SQL 片段回应。
     */
    private static PicServiceMapper mapperWithRange(int minId, int maxId, long validCount) {
        PicServiceMapper mapper = mock(PicServiceMapper.class);
        when(mapper.selectOne(any())).thenAnswer(invocation -> {
            String sql = ((Wrapper<?>) invocation.getArgument(0)).getSqlSegment();
            if (sql.contains("group_id")) {
                return pic(1, sql.contains("DESC") ? 999L : 1L);
            }
            return pic(sql.contains("DESC") ? maxId : minId, 1L);
        });
        when(mapper.selectValidPicCount()).thenReturn(validCount);
        return mapper;
    }

    private static CacheService service(PicServiceMapper mapper) {
        return new CacheService(mock(GroupServiceMapper.class), mapper, mock(EnvContext.class));
    }

    @Test
    void getRandomId_shouldSeekWithinCachedRange() throws SQLException {
        PicServiceMapper mapper = mapperWithRange(1, 100, 100);
        when(mapper.selectFirstValidIdSince(anyLong())).thenReturn(42L);
        CacheService service = service(mapper);
        service.cachePicId();

        assertEquals(42L, service.getRandomId());

        ArgumentCaptor<Long> point = ArgumentCaptor.forClass(Long.class);
        verify(mapper).selectFirstValidIdSince(point.capture());
        assertTrue(point.getValue() >= 1 && point.getValue() <= 100,
                "随机起点应落在缓存区间内，实际 " + point.getValue());
    }

    @Test
    void getRandomId_shouldFallBack_whenSeekMisses() throws SQLException {
        PicServiceMapper mapper = mapperWithRange(1, 100, 100);
        when(mapper.selectFirstValidIdSince(anyLong())).thenReturn(null, 7L);
        CacheService service = service(mapper);
        service.cachePicId();

        assertEquals(7L, service.getRandomId(), "起跳落空时必须兜底，不能让随机接口返回空");
        verify(mapper, times(2)).selectFirstValidIdSince(anyLong());
    }

    @Test
    void getRandomId_shouldReturnNull_whenNoValidPic() throws SQLException {
        PicServiceMapper mapper = mapperWithRange(0, 0, 0);
        CacheService service = service(mapper);
        service.cachePicId();

        assertNull(service.getRandomId());
        verify(mapper, never()).selectFirstValidIdSince(anyLong());
    }

    @Test
    void envInfo_shouldReportCachedValidCount() throws SQLException {
        CacheService service = service(mapperWithRange(1, 100, 841_140L));
        service.cachePicId();

        assertEquals(841_140L, service.getDefaultEnvInfo().getPicCount());
    }
}
