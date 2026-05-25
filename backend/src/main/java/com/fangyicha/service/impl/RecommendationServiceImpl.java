package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangyicha.dto.RecommendationDTO;
import com.fangyicha.entity.Customer;
import com.fangyicha.entity.Developer;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.CustomerMapper;
import com.fangyicha.mapper.DeveloperMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房产推荐服务实现
 *
 * 算法：
 * 1. 加载客户偏好区域（preferredLocations，逗号分隔）
 * 2. 按每个偏好区域 LIKE 匹配房产
 * 3. 按预算中点排序（越接近预算中点越优先）
 * 4. 确定推荐理由
 * 5. 最多返回6条
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final CustomerMapper customerMapper;
    private final PropertyMapper propertyMapper;
    private final DeveloperMapper developerMapper;

    public RecommendationServiceImpl(CustomerMapper customerMapper,
                                     PropertyMapper propertyMapper,
                                     DeveloperMapper developerMapper) {
        this.customerMapper = customerMapper;
        this.propertyMapper = propertyMapper;
        this.developerMapper = developerMapper;
    }

    @Override
    public List<RecommendationDTO> getRecommendations(Long customerId) {
        // 加载客户信息
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            return Collections.emptyList();
        }

        // 解析偏好区域
        String preferredLocationsStr = customer.getPreferredLocations();
        if (preferredLocationsStr == null || preferredLocationsStr.trim().isEmpty()) {
            // 没有偏好区域，返回所有在售房产中价格最近的
            return recommendByBudgetOnly(customer);
        }
        String[] preferredLocations = preferredLocationsStr.split(",");

        // 获取预算中点
        BigDecimal budgetMid = getBudgetMidpoint(customer);

        // 收集匹配的房产
        Map<Long, Property> matchedProperties = new LinkedHashMap<>();

        for (String location : preferredLocations) {
            String loc = location.trim();
            if (loc.isEmpty()) continue;

            LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Property::getLocation, loc)
                    .eq(Property::getStatus, "在售");
            List<Property> props = propertyMapper.selectList(wrapper);
            for (Property p : props) {
                if (!matchedProperties.containsKey(p.getId())) {
                    matchedProperties.put(p.getId(), p);
                }
            }
        }

        // 按预算排序（计算预算偏差）
        List<Map.Entry<Long, Property>> sorted = matchedProperties.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> {
                    Property p = e.getValue();
                    BigDecimal price = p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO;
                    return Math.abs(price.subtract(budgetMid).doubleValue());
                }))
                .limit(6)
                .collect(Collectors.toList());

        // 构建推荐结果
        List<RecommendationDTO> result = new ArrayList<>();
        Set<String> locationSet = new HashSet<>(Arrays.asList(preferredLocations));
        for (Map.Entry<Long, Property> entry : sorted) {
            Property p = entry.getValue();
            RecommendationDTO dto = new RecommendationDTO();
            dto.setPropertyId(p.getId());
            dto.setPropertyName(p.getPropertyName());
            dto.setLocation(p.getLocation());
            dto.setPricePerSqm(p.getPricePerSqm());
            dto.setTotalPrice(p.getTotalPrice());
            dto.setAreaSqm(p.getAreaSqm());
            dto.setFloorPlanType(p.getFloorPlanType());
            dto.setDecoration(p.getDecoration());
            dto.setImageUrls(p.getImageUrls());

            // 填充开发商名称
            Developer dev = developerMapper.selectById(p.getDeveloperId());
            if (dev != null) {
                dto.setDeveloperName(dev.getCompanyName());
            }

            // 确定推荐理由
            dto.setReason(buildReason(p, customer, locationSet));

            result.add(dto);
        }

        return result;
    }

    /**
     * 没有偏好区域时的默认推荐
     */
    private List<RecommendationDTO> recommendByBudgetOnly(Customer customer) {
        BigDecimal budgetMid = getBudgetMidpoint(customer);

        LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Property::getStatus, "在售");
        List<Property> allProperties = propertyMapper.selectList(wrapper);

        List<RecommendationDTO> result = allProperties.stream()
                .sorted(Comparator.comparingDouble(p -> {
                    BigDecimal price = p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO;
                    return Math.abs(price.subtract(budgetMid).doubleValue());
                }))
                .limit(6)
                .map(p -> {
                    RecommendationDTO dto = new RecommendationDTO();
                    dto.setPropertyId(p.getId());
                    dto.setPropertyName(p.getPropertyName());
                    dto.setLocation(p.getLocation());
                    dto.setPricePerSqm(p.getPricePerSqm());
                    dto.setTotalPrice(p.getTotalPrice());
                    dto.setAreaSqm(p.getAreaSqm());
                    dto.setFloorPlanType(p.getFloorPlanType());
                    dto.setDecoration(p.getDecoration());
                    dto.setImageUrls(p.getImageUrls());
                    Developer dev = developerMapper.selectById(p.getDeveloperId());
                    if (dev != null) {
                        dto.setDeveloperName(dev.getCompanyName());
                    }
                    // 预算接近推荐
                    dto.setReason("符合您的预算范围");
                    return dto;
                })
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 获取预算中点
     */
    private BigDecimal getBudgetMidpoint(Customer customer) {
        BigDecimal min = customer.getBudgetMin() != null ? customer.getBudgetMin() : BigDecimal.ZERO;
        BigDecimal max = customer.getBudgetMax() != null ? customer.getBudgetMax() : BigDecimal.valueOf(10000000);
        return min.add(max).divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 构建推荐理由
     */
    private String buildReason(Property property, Customer customer, Set<String> preferredLocations) {
        // 检查位置是否在偏好区域
        if (preferredLocations != null) {
            for (String loc : preferredLocations) {
                if (property.getLocation() != null && property.getLocation().contains(loc.trim())) {
                    return "在您的偏好区域「" + loc.trim() + "」内";
                }
            }
        }

        // 检查预算
        if (customer.getBudgetMin() != null && customer.getBudgetMax() != null
                && property.getTotalPrice() != null) {
            if (property.getTotalPrice().compareTo(customer.getBudgetMin()) >= 0
                    && property.getTotalPrice().compareTo(customer.getBudgetMax()) <= 0) {
                return "符合您的预算范围";
            }
        }

        // 装修匹配
        if (customer.getIntention() != null && customer.getIntention().contains("精装")
                && "精装".equals(property.getDecoration())) {
            return "精装交付，省心入住";
        }

        return "热门在售楼盘";
    }
}
