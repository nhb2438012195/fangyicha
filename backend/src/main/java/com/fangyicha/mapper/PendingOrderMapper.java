package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.PendingOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI待确认订单数据访问层
 */
@Mapper
public interface PendingOrderMapper extends BaseMapper<PendingOrder> {
}
