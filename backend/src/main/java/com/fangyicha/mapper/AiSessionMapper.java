package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.AiSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI会话数据访问层
 */
@Mapper
public interface AiSessionMapper extends BaseMapper<AiSession> {
}
