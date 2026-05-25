package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.entity.Developer;

import java.util.Map;

/**
 * 开发商服务接口
 */
public interface DeveloperService extends IService<Developer> {

    /**
     * 根据用户名查询
     */
    Developer getByUsername(String username);

    /**
     * 分页查询开发商列表
     */
    Page<Developer> getDeveloperPage(String keyword, Integer page, Integer pageSize);

    /**
     * 更新公司信息
     */
    boolean updateProfile(Long id, Developer developer);

    /**
     * 获取仪表盘统计数据
     */
    Map<String, Object> getDashboardStats(Long developerId);
}
