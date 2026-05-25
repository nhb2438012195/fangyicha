package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户数据访问层
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
