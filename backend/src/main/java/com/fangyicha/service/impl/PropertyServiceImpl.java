package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.dto.PropertyQueryRequest;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 房产服务实现
 */
@Slf4j
@Service
public class PropertyServiceImpl extends ServiceImpl<PropertyMapper, Property> implements PropertyService {

    private final PropertyMapper propertyMapper;

    public PropertyServiceImpl(PropertyMapper propertyMapper) {
        this.propertyMapper = propertyMapper;
    }

    @Override
    public Page<Property> queryProperties(PropertyQueryRequest request) {
        Page<Property> pageParam = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Property> wrapper = buildQueryWrapper(request);

        // 排序处理
        if (StringUtils.hasText(request.getSortBy()) && StringUtils.hasText(request.getSortOrder())) {
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            switch (request.getSortBy()) {
                case "price_per_sqm":
                    wrapper.orderBy(true, isAsc, Property::getPricePerSqm);
                    break;
                case "total_price":
                    wrapper.orderBy(true, isAsc, Property::getTotalPrice);
                    break;
                case "area_sqm":
                    wrapper.orderBy(true, isAsc, Property::getAreaSqm);
                    break;
                case "vacancy_rate":
                    wrapper.orderBy(true, isAsc, Property::getVacancyRate);
                    break;
                case "created_time":
                    wrapper.orderBy(true, isAsc, Property::getCreatedTime);
                    break;
                default:
                    wrapper.orderByDesc(Property::getCreatedTime);
            }
        } else {
            wrapper.orderByDesc(Property::getCreatedTime);
        }

        return propertyMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Property> getMyProperties(Long developerId, PropertyQueryRequest request) {
        if (request == null) {
            request = new PropertyQueryRequest();
        }
        request.setDeveloperId(developerId);
        return queryProperties(request);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Property> buildQueryWrapper(PropertyQueryRequest request) {
        LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();

        if (request == null) {
            return wrapper;
        }

        // 开发商ID精确匹配
        if (request.getDeveloperId() != null) {
            wrapper.eq(Property::getDeveloperId, request.getDeveloperId());
        }

        // 关键字模糊匹配（楼盘名称+位置）
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Property::getPropertyName, request.getKeyword())
                    .or()
                    .like(Property::getLocation, request.getKeyword()));
        }

        // 位置模糊匹配
        if (StringUtils.hasText(request.getLocation())) {
            wrapper.like(Property::getLocation, request.getLocation());
        }

        // 楼层范围
        if (request.getFloorMin() != null) {
            wrapper.ge(Property::getFloorMin, request.getFloorMin());
        }
        if (request.getFloorMax() != null) {
            wrapper.le(Property::getFloorMax, request.getFloorMax());
        }

        // 户型（多选，逗号分隔）
        if (StringUtils.hasText(request.getFloorPlanType())) {
            String[] types = request.getFloorPlanType().split(",");
            wrapper.and(w -> {
                for (String type : types) {
                    w.or().eq(Property::getFloorPlanType, type.trim());
                }
            });
        }

        // 单价范围
        if (request.getPriceMin() != null) {
            wrapper.ge(Property::getPricePerSqm, request.getPriceMin());
        }
        if (request.getPriceMax() != null) {
            wrapper.le(Property::getPricePerSqm, request.getPriceMax());
        }

        // 总价范围
        if (request.getTotalPriceMin() != null) {
            wrapper.ge(Property::getTotalPrice, request.getTotalPriceMin());
        }
        if (request.getTotalPriceMax() != null) {
            wrapper.le(Property::getTotalPrice, request.getTotalPriceMax());
        }

        // 空置率范围
        if (request.getVacancyRateMin() != null) {
            wrapper.ge(Property::getVacancyRate, request.getVacancyRateMin());
        }
        if (request.getVacancyRateMax() != null) {
            wrapper.le(Property::getVacancyRate, request.getVacancyRateMax());
        }

        // 状态
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Property::getStatus, request.getStatus());
        }

        // 装修情况
        if (StringUtils.hasText(request.getDecoration())) {
            wrapper.eq(Property::getDecoration, request.getDecoration());
        }

        return wrapper;
    }

    /**
     * 自动计算空置率
     */
    private void calculateVacancyRate(Property property) {
        if (property.getTotalUnits() != null && property.getTotalUnits() > 0
                && property.getVacantUnits() != null) {
            BigDecimal rate = BigDecimal.valueOf(property.getVacantUnits())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(property.getTotalUnits()), 2, RoundingMode.HALF_UP);
            property.setVacancyRate(rate);
        } else {
            property.setVacancyRate(BigDecimal.ZERO);
        }
    }

    @Override
    @Transactional
    public Property createProperty(Long developerId, Property property) {
        property.setDeveloperId(developerId);
        calculateVacancyRate(property);
        if (property.getStatus() == null) {
            property.setStatus("在售");
        }
        propertyMapper.insert(property);
        log.info("开发商{}创建房产: {}, id={}", developerId, property.getPropertyName(), property.getId());
        return property;
    }

    @Override
    @Transactional
    public boolean updateProperty(Long id, Long developerId, Property property) {
        Property existing = propertyMapper.selectById(id);
        if (existing == null || !existing.getDeveloperId().equals(developerId)) {
            return false;
        }
        // 更新字段
        existing.setPropertyName(property.getPropertyName());
        existing.setLocation(property.getLocation());
        existing.setLongitude(property.getLongitude());
        existing.setLatitude(property.getLatitude());
        existing.setFloorMin(property.getFloorMin());
        existing.setFloorMax(property.getFloorMax());
        existing.setFloorPlanType(property.getFloorPlanType());
        existing.setTotalUnits(property.getTotalUnits());
        existing.setVacantUnits(property.getVacantUnits());
        calculateVacancyRate(existing);
        existing.setPricePerSqm(property.getPricePerSqm());
        existing.setTotalPrice(property.getTotalPrice());
        existing.setAreaSqm(property.getAreaSqm());
        existing.setDecoration(property.getDecoration());
        existing.setStatus(property.getStatus());
        existing.setDescription(property.getDescription());
        existing.setImageUrls(property.getImageUrls());

        int result = propertyMapper.updateById(existing);
        log.info("更新房产: id={}, name={}", id, existing.getPropertyName());
        return result > 0;
    }

    @Override
    @Transactional
    public boolean deleteProperty(Long id, Long developerId) {
        Property existing = propertyMapper.selectById(id);
        if (existing == null || !existing.getDeveloperId().equals(developerId)) {
            return false;
        }
        int result = propertyMapper.deleteById(id);
        log.info("删除房产: id={}, name={}", id, existing.getPropertyName());
        return result > 0;
    }

    @Override
    public List<Map<String, Object>> getVacancyByLocation(Long developerId) {
        return propertyMapper.selectVacancyByLocation(developerId);
    }

    @Override
    public List<Map<String, Object>> getVacancyByType(Long developerId) {
        return propertyMapper.selectVacancyByType(developerId);
    }

    @Override
    public List<Map<String, Object>> getVacancyByFloor(Long developerId) {
        return propertyMapper.selectVacancyByFloor(developerId);
    }
}
