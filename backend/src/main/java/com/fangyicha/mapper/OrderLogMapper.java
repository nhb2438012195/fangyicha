package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.OrderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单日志数据访问层
 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}
