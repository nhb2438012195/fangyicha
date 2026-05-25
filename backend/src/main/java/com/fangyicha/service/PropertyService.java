package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.dto.PropertyQueryRequest;
import com.fangyicha.entity.Property;

import java.util.List;
import java.util.Map;

/**
 * 房产服务接口
 */
public interface PropertyService extends IService<Property> {

    /**
     * 多条件分页查询房产
     */
    Page<Property> queryProperties(PropertyQueryRequest request);

    /**
     * 开发商查看自己的房产列表
     */
    Page<Property> getMyProperties(Long developerId, PropertyQueryRequest request);

    /**
     * 创建房产（自动计算空置率）
     */
    boolean createProperty(Long developerId, Property property);

    /**
     * 更新房产（自动计算空置率）
     */
    boolean updateProperty(Long id, Long developerId, Property property);

    /**
     * 删除房产（校验归属）
     */
    boolean deleteProperty(Long id, Long developerId);

    /**
     * 按位置统计空置率（柱状图数据）
     */
    List<Map<String, Object>> getVacancyByLocation(Long developerId);

    /**
     * 按户型统计空置率（分组柱状图数据）
     */
    List<Map<String, Object>> getVacancyByType(Long developerId);

    /**
     * 楼层与空置率散点数据
     */
    List<Map<String, Object>> getVacancyByFloor(Long developerId);
}
