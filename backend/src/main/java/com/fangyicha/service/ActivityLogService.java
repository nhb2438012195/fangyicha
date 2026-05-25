package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.entity.ActivityLog;

/**
 * 活动日志服务接口
 */
public interface ActivityLogService extends IService<ActivityLog> {

    /**
     * 记录操作日志
     */
    void log(Long actorId, String actorRole, String action, String entityType, Long entityId, String detail);

    /**
     * 查询操作日志（按操作人）
     */
    Page<ActivityLog> getLogsByActor(Long actorId, String actorRole, Integer page, Integer pageSize);
}
