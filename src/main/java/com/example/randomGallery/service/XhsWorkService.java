package com.example.randomGallery.service;

import com.example.randomGallery.entity.VO.RandomGifVO;
import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import org.springframework.stereotype.Service;

/**
 * 作品查询服务
 */
@Service
public interface XhsWorkService {

    /**
     * 分页查询作品列表（支持筛选）
     * 
     * @param page     页码（从0开始）
     * @param pageSize 每页数量
     * @param authorId 作者ID（可选）
     * @param tagId    标签ID（可选）
     * @return 作品分页数据
     */
    XhsWorkPageVO pageXhsWorksWithFilter(int page, int pageSize, String authorId, Long tagId, String str, Integer seed, boolean skipHeicConversion);

    /**
     * 获取作品详情
     * 
     * @param workId 作品ID
     * @return 作品详情
     */
    XhsWorkDetailVO getXhsWorkDetail(String workId, boolean skipHeicConversion);

    /**
     * 删除作品（软删除）
     * 
     * @param workId 作品ID
     */
    void deleteWork(String workId);

    /**
     * 删除媒体（软删除）
     * 
     * @param id 媒体URL (或者根据ID删除，Plan说是根据MediaID，这里确认用MediaID更安全)
     */
    void deleteMedia(Long id);

    /**
     * 获取随机GIF
     *
     * @return 随机GIF数据，如果没有GIF则返回null
     */
    RandomGifVO getRandomGif();

    /**
     * 获取随机GIF（排除已看过的ID）
     *
     * @param excludeIds 需要排除的媒体ID列表（已浏览/已收藏）
     * @return 随机GIF数据，如果没有可用GIF则返回null
     */
    RandomGifVO getRandomGif(java.util.List<Long> excludeIds);

    /**
     * 上报媒体外链失效，标记为失效并从随机池剔除
     *
     * @param id 媒体ID
     */
    void reportDead(Long id);

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     *
     * @param mediaId   媒体ID
     * @param mediaType 媒体类型：gif / pic
     * @return 切换后的收藏状态：true=已收藏，false=未收藏
     */
    boolean toggleFavorite(Long mediaId, String mediaType);

    /**
     * 查询是否已收藏
     */
    boolean isFavorite(Long mediaId, String mediaType);

    /**
     * 获取收藏列表（分页，按时间倒序）
     */
    java.util.List<RandomGifVO> getFavorites(int page, int pageSize);

    /**
     * 随机获取同一作品的一整组套图GIF
     * 
     * @return 同一作品下的一组GIF列表
     */
    java.util.List<RandomGifVO> getRandomGifGroup();

    /**
     * 获取所有GIF的ID列表
     * 
     * @return 所有可用GIF的ID列表
     */
    java.util.List<Long> getAllGifIds();

    /**
     * 根据ID获取GIF详情
     * 
     * @param id GIF媒体ID
     * @return GIF详情数据，如果未找到则返回null
     */
    RandomGifVO getGifById(Long id);
}
