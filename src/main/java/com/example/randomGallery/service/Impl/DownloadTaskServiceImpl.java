package com.example.randomGallery.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.randomGallery.entity.DO.XhsDownloadTaskDO;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.XhsDownloadTaskVO;
import com.example.randomGallery.entity.common.DownloadTaskStatusEnum;
import com.example.randomGallery.entity.common.PageResult;
import com.example.randomGallery.exception.NotFoundException;
import com.example.randomGallery.service.DownloadTaskService;
import com.example.randomGallery.service.mapper.XhsDownloadTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 下载任务历史记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadTaskServiceImpl implements DownloadTaskService {

    private final XhsDownloadTaskMapper downloadTaskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTask(DownLoadQry qry) {
        XhsDownloadTaskDO task = new XhsDownloadTaskDO();
        task.setUrl(qry.getUrl());
        task.setParamsDownload(Boolean.TRUE.equals(qry.getDownload()));
        task.setParamsIndex(qry.getIndex() == null ? null : JSONUtil.toJsonStr(qry.getIndex()));
        task.setParamsCookie(qry.getCookie());
        task.setParamsProxy(qry.getProxy());
        task.setParamsSkip(Boolean.TRUE.equals(qry.getSkip()));
        task.setStatus(DownloadTaskStatusEnum.WAITING);
        task.setRetryCount(0);
        task.setCreateTime(LocalDateTime.now());
        downloadTaskMapper.insert(task);
        log.info("新增下载任务历史记录, id: {}, url: {}", task.getId(), qry.getUrl());
        return task.getId();
    }

    @Override
    public PageResult<XhsDownloadTaskVO> pageHistory(int page, int size, Integer status) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        Page<XhsDownloadTaskDO> p = new Page<>(currentPage, pageSize);
        
        var query = Wrappers.<XhsDownloadTaskDO>lambdaQuery();
        if (status != null) {
            DownloadTaskStatusEnum statusEnum = DownloadTaskStatusEnum.fromValue(status);
            if (statusEnum != null) {
                if (statusEnum == DownloadTaskStatusEnum.COMPLETED) {
                    // 已完成包含 COMPLETED 与 UPDATED
                    query.in(XhsDownloadTaskDO::getStatus, DownloadTaskStatusEnum.COMPLETED, DownloadTaskStatusEnum.UPDATED);
                } else {
                    query.eq(XhsDownloadTaskDO::getStatus, statusEnum);
                }
            }
        }
        query.orderByDesc(XhsDownloadTaskDO::getCreateTime)
             .orderByDesc(XhsDownloadTaskDO::getId);

        Page<XhsDownloadTaskDO> result = downloadTaskMapper.selectPage(p, query);
        List<XhsDownloadTaskVO> list = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public com.example.randomGallery.entity.VO.DownloadTaskStatsVO getStats() {
        long waiting = downloadTaskMapper.selectCount(Wrappers.<XhsDownloadTaskDO>lambdaQuery().eq(XhsDownloadTaskDO::getStatus, DownloadTaskStatusEnum.WAITING));
        long completed = downloadTaskMapper.selectCount(Wrappers.<XhsDownloadTaskDO>lambdaQuery().in(XhsDownloadTaskDO::getStatus, DownloadTaskStatusEnum.COMPLETED, DownloadTaskStatusEnum.UPDATED));
        long failed = downloadTaskMapper.selectCount(Wrappers.<XhsDownloadTaskDO>lambdaQuery().eq(XhsDownloadTaskDO::getStatus, DownloadTaskStatusEnum.FAILED));
        long total = waiting + completed + failed;
        return com.example.randomGallery.entity.VO.DownloadTaskStatsVO.builder()
                .total(total)
                .waitingCount(waiting)
                .completedCount(completed)
                .failedCount(failed)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        if (id == null) return;
        downloadTaskMapper.deleteById(id);
        log.info("已删除下载任务记录, id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long id) {
        XhsDownloadTaskDO task = getById(id);
        if (task == null) {
            throw new NotFoundException("下载任务不存在");
        }
        if (task.getStatus() != DownloadTaskStatusEnum.FAILED) {
            throw new IllegalStateException("仅失败的任务支持重试");
        }
        task.setStatus(DownloadTaskStatusEnum.WAITING);
        task.setErrorMessage(null);
        task.setFinishTime(null);
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        downloadTaskMapper.updateById(task);
        log.info("下载任务重试, id: {}, 重试次数: {}", id, task.getRetryCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCompleted(Long id, String workId, String workTitle, String workUrl) {
        markFinished(id, DownloadTaskStatusEnum.COMPLETED, workId, workTitle, workUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markUpdated(Long id, String workId, String workTitle, String workUrl) {
        markFinished(id, DownloadTaskStatusEnum.UPDATED, workId, workTitle, workUrl);
    }

    private void markFinished(Long id, DownloadTaskStatusEnum status,
            String workId, String workTitle, String workUrl) {
        XhsDownloadTaskDO task = new XhsDownloadTaskDO();
        task.setId(id);
        task.setStatus(status);
        task.setWorkId(workId);
        task.setWorkTitle(workTitle);
        task.setWorkUrl(workUrl);
        task.setFinishTime(LocalDateTime.now());
        downloadTaskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long id, String errorMessage) {
        XhsDownloadTaskDO task = new XhsDownloadTaskDO();
        task.setId(id);
        task.setStatus(DownloadTaskStatusEnum.FAILED);
        task.setErrorMessage(errorMessage);
        task.setFinishTime(LocalDateTime.now());
        downloadTaskMapper.updateById(task);
    }

    @Override
    public List<Long> listPendingTaskIds() {
        List<XhsDownloadTaskDO> list = downloadTaskMapper.selectList(
                Wrappers.<XhsDownloadTaskDO>lambdaQuery()
                        .eq(XhsDownloadTaskDO::getStatus, DownloadTaskStatusEnum.WAITING)
                        .orderByAsc(XhsDownloadTaskDO::getCreateTime)
                        .orderByAsc(XhsDownloadTaskDO::getId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(XhsDownloadTaskDO::getId).collect(Collectors.toList());
    }

    @Override
    public XhsDownloadTaskDO getById(Long id) {
        return downloadTaskMapper.selectById(id);
    }

    private XhsDownloadTaskVO convertToVO(XhsDownloadTaskDO task) {
        XhsDownloadTaskVO vo = new XhsDownloadTaskVO();
        BeanUtil.copyProperties(task, vo);
        DownloadTaskStatusEnum status = task.getStatus();
        vo.setStatus(status == null ? null : status.getValue());
        vo.setStatusName(status == null ? null : status.getLabel());
        return vo;
    }
}
