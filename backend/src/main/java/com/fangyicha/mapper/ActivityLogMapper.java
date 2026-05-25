package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.ActivityLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动日志数据访问层
 */
@Mapper
public interface ActivityLogMapper extends BaseMapper<ActivityLog> {
}
