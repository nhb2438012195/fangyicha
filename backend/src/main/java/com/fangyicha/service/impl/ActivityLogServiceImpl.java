package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.entity.ActivityLog;
import com.fangyicha.mapper.ActivityLogMapper;
import com.fangyicha.service.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 活动日志服务实现
 */
@Slf4j
@Service
public class ActivityLogServiceImpl extends ServiceImpl<ActivityLogMapper, ActivityLog> implements ActivityLogService {

    private final ActivityLogMapper activityLogMapper;

    public ActivityLogServiceImpl(ActivityLogMapper activityLogMapper) {
        this.activityLogMapper = activityLogMapper;
    }

    @Override
    public void log(Long actorId, String actorRole, String action, String entityType, Long entityId, String detail) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setActorId(actorId);
        activityLog.setActorRole(actorRole);
        activityLog.setAction(action);
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(entityId);
        activityLog.setDetail(detail);
        activityLogMapper.insert(activityLog);
    }

    @Override
    public Page<ActivityLog> getLogsByActor(Long actorId, String actorRole, Integer page, Integer pageSize) {
        Page<ActivityLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<ActivityLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityLog::getActorId, actorId)
                .eq(ActivityLog::getActorRole, actorRole);
        wrapper.orderByDesc(ActivityLog::getCreatedTime);
        return activityLogMapper.selectPage(pageParam, wrapper);
    }
}
