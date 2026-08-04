package com.example.randomGallery.service;

import com.example.randomGallery.entity.DO.XhsDownloadTaskDO;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.XhsDownloadTaskVO;
import com.example.randomGallery.entity.common.PageResult;

import java.util.List;

/**
 * 下载任务历史记录服务
 */
public interface DownloadTaskService {

    /**
     * 新增下载任务历史记录（状态=等待中）
     *
     * @param qry 下载参数
     * @return 任务ID
     */
    Long addTask(DownLoadQry qry);

    /**
     * 分页查询下载历史（按添加时间倒序）
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     */
    PageResult<XhsDownloadTaskVO> pageHistory(int page, int size);

    /**
     * 重试失败的任务（仅 status=失败 可重试，置回等待中并累加重试次数）
     *
     * @param id 任务ID
     */
    void retryTask(Long id);

    /**
     * 标记任务为已完成并回填作品信息
     */
    void markCompleted(Long id, String workId, String workTitle, String workUrl);

    /**
     * 标记任务为失败并记录原因
     */
    void markFailed(Long id, String errorMessage);

    /**
     * 查询所有等待中的任务ID（用于应用启动时恢复）
     */
    List<Long> listPendingTaskIds();

    /**
     * 按ID查询任务
     */
    XhsDownloadTaskDO getById(Long id);
}
