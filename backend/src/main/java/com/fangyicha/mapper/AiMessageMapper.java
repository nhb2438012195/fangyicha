package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI消息数据访问层
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
