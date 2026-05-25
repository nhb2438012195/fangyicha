package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.Developer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开发商数据访问层
 */
@Mapper
public interface DeveloperMapper extends BaseMapper<Developer> {
}
