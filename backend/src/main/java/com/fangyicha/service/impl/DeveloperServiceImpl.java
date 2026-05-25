package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.common.Constants;
import com.fangyicha.entity.Developer;
import com.fangyicha.entity.Order;
import com.fangyicha.entity.Property;
import com.fangyicha.entity.Suggestion;
import com.fangyicha.mapper.DeveloperMapper;
import com.fangyicha.mapper.OrderMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.mapper.SuggestionMapper;
import com.fangyicha.service.DeveloperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开发商服务实现
 */
@Slf4j
@Service
public class DeveloperServiceImpl extends ServiceImpl<DeveloperMapper, Developer> implements DeveloperService {

    private final DeveloperMapper developerMapper;
    private final PropertyMapper propertyMapper;
    private final SuggestionMapper suggestionMapper;
    private final OrderMapper orderMapper;

    public DeveloperServiceImpl(DeveloperMapper developerMapper,
                                PropertyMapper propertyMapper,
                                SuggestionMapper suggestionMapper,
                                OrderMapper orderMapper) {
        this.developerMapper = developerMapper;
        this.propertyMapper = propertyMapper;
        this.suggestionMapper = suggestionMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public Developer getByUsername(String username) {
        LambdaQueryWrapper<Developer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Developer::getUsername, username);
        return developerMapper.selectOne(wrapper);
    }

    @Override
    public Page<Developer> getDeveloperPage(String keyword, Integer page, Integer pageSize) {
        Page<Developer> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Developer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Developer::getStatus, 1);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Developer::getCompanyName, keyword.trim());
        }
        wrapper.orderByDesc(Developer::getCreatedTime);
        return developerMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional
    public boolean updateProfile(Long id, Developer developer) {
        Developer existing = developerMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        // 只更新允许修改的字段
        existing.setCompanyName(developer.getCompanyName());
        existing.setContactPerson(developer.getContactPerson());
        existing.setPhone(developer.getPhone());
        existing.setEmail(developer.getEmail());
        existing.setAddress(developer.getAddress());
        existing.setBusinessLicense(developer.getBusinessLicense());
        existing.setDescription(developer.getDescription());
        return developerMapper.updateById(existing) > 0;
    }

    @Override
    public Map<String, Object> getDashboardStats(Long developerId) {
        Map<String, Object> stats = new HashMap<>();

        // 房产总数
        LambdaQueryWrapper<Property> propertyWrapper = new LambdaQueryWrapper<>();
        propertyWrapper.eq(Property::getDeveloperId, developerId);
        Long propertyCount = propertyMapper.selectCount(propertyWrapper);
        stats.put("propertyCount", propertyCount);

        // 总户数
        List<Property> properties = propertyMapper.selectList(propertyWrapper);
        int totalUnits = properties.stream().mapToInt(p -> p.getTotalUnits() != null ? p.getTotalUnits() : 0).sum();
        int vacantUnits = properties.stream().mapToInt(p -> p.getVacantUnits() != null ? p.getVacantUnits() : 0).sum();
        stats.put("totalUnits", totalUnits);
        stats.put("vacantUnits", vacantUnits);

        // 平均空置率
        if (totalUnits > 0) {
            BigDecimal avgVacancyRate = BigDecimal.valueOf(vacantUnits)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalUnits), 2, RoundingMode.HALF_UP);
            stats.put("avgVacancyRate", avgVacancyRate);
        } else {
            stats.put("avgVacancyRate", BigDecimal.ZERO);
        }

        // 各状态房产数量
        LambdaQueryWrapper<Property> onSaleWrapper = new LambdaQueryWrapper<>();
        onSaleWrapper.eq(Property::getDeveloperId, developerId)
                .eq(Property::getStatus, Constants.PROPERTY_STATUS_ON_SALE);
        stats.put("onSaleCount", propertyMapper.selectCount(onSaleWrapper));

        LambdaQueryWrapper<Property> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Property::getDeveloperId, developerId)
                .eq(Property::getStatus, Constants.PROPERTY_STATUS_PENDING);
        stats.put("pendingCount", propertyMapper.selectCount(pendingWrapper));

        // 最近建议数
        LambdaQueryWrapper<Suggestion> suggestionWrapper = new LambdaQueryWrapper<>();
        suggestionWrapper.eq(Suggestion::getDeveloperId, developerId)
                .eq(Suggestion::getStatus, Constants.SUGGESTION_PENDING);
        Long pendingSuggestions = suggestionMapper.selectCount(suggestionWrapper);
        stats.put("pendingSuggestions", pendingSuggestions);

        // 订单统计
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getDeveloperId, developerId);
        stats.put("orderCount", orderMapper.selectCount(orderWrapper));

        LambdaQueryWrapper<Order> pendingOrderWrapper = new LambdaQueryWrapper<>();
        pendingOrderWrapper.eq(Order::getDeveloperId, developerId)
                .eq(Order::getStatus, Constants.ORDER_PENDING_PAYMENT);
        stats.put("pendingOrderCount", orderMapper.selectCount(pendingOrderWrapper));

        LambdaQueryWrapper<Order> paidOrderWrapper = new LambdaQueryWrapper<>();
        paidOrderWrapper.eq(Order::getDeveloperId, developerId)
                .eq(Order::getStatus, Constants.ORDER_PAID);
        stats.put("paidOrderCount", orderMapper.selectCount(paidOrderWrapper));

        return stats;
    }
}
