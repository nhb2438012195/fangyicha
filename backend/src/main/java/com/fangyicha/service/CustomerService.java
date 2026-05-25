package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.dto.RegisterRequest;
import com.fangyicha.entity.Customer;

import java.util.Map;

/**
 * 客户服务接口
 */
public interface CustomerService extends IService<Customer> {

    /**
     * 根据用户名查询
     */
    Customer getByUsername(String username);

    /**
     * 客户注册
     */
    Customer register(RegisterRequest request);

    /**
     * 检查用户名是否已存在
     */
    boolean checkUsernameExists(String username);

    /**
     * 更新客户资料
     */
    boolean updateProfile(Long id, Customer customer);

    /**
     * 获取仪表盘统计数据
     */
    Map<String, Object> getDashboardStats(Long customerId);
}
