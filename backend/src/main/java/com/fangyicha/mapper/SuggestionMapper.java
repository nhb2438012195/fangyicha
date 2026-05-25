package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.Suggestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 建议/购房意向数据访问层
 */
@Mapper
public interface SuggestionMapper extends BaseMapper<Suggestion> {
}
