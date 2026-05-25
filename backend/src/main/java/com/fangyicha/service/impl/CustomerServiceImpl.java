package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.dto.RegisterRequest;
import com.fangyicha.entity.Customer;
import com.fangyicha.entity.Order;
import com.fangyicha.entity.Suggestion;
import com.fangyicha.mapper.CustomerMapper;
import com.fangyicha.mapper.OrderMapper;
import com.fangyicha.mapper.SuggestionMapper;
import com.fangyicha.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户服务实现
 */
@Slf4j
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private final CustomerMapper customerMapper;
    private final SuggestionMapper suggestionMapper;
    private final OrderMapper orderMapper;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerMapper customerMapper,
                               SuggestionMapper suggestionMapper,
                               OrderMapper orderMapper,
                               PasswordEncoder passwordEncoder) {
        this.customerMapper = customerMapper;
        this.suggestionMapper = suggestionMapper;
        this.orderMapper = orderMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Customer getByUsername(String username) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getUsername, username);
        return customerMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public Customer register(RegisterRequest request) {
        // 校验用户名唯一性
        if (checkUsernameExists(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 校验密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 校验协议同意
        if (request.getAgreement() == null || !request.getAgreement()) {
            throw new RuntimeException("请同意用户协议");
        }

        // 创建客户
        Customer customer = new Customer();
        customer.setUsername(request.getUsername());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setRealName(request.getRealName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setIdCard(request.getIdCard());
        customer.setStatus(1);

        customerMapper.insert(customer);
        log.info("新客户注册成功: username={}", customer.getUsername());
        return customer;
    }

    @Override
    public boolean checkUsernameExists(String username) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getUsername, username);
        return customerMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean updateProfile(Long id, Customer customer) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        // 只更新允许修改的字段
        existing.setPhone(customer.getPhone());
        existing.setEmail(customer.getEmail());
        existing.setIntention(customer.getIntention());
        existing.setPreferredLocations(customer.getPreferredLocations());
        existing.setBudgetMin(customer.getBudgetMin());
        existing.setBudgetMax(customer.getBudgetMax());
        existing.setUrgency(customer.getUrgency());
        return customerMapper.updateById(existing) > 0;
    }

    @Override
    public Map<String, Object> getDashboardStats(Long customerId) {
        Map<String, Object> stats = new HashMap<>();

        // 建议数量
        LambdaQueryWrapper<Suggestion> suggestionWrapper = new LambdaQueryWrapper<>();
        suggestionWrapper.eq(Suggestion::getCustomerId, customerId);
        Long suggestionCount = suggestionMapper.selectCount(suggestionWrapper);
        stats.put("suggestionCount", suggestionCount);

        // 待回复建议数
        LambdaQueryWrapper<Suggestion> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Suggestion::getCustomerId, customerId)
                .eq(Suggestion::getStatus, "待回复");
        stats.put("pendingCount", suggestionMapper.selectCount(pendingWrapper));

        // 订单统计
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getCustomerId, customerId);
        stats.put("orderCount", orderMapper.selectCount(orderWrapper));

        LambdaQueryWrapper<Order> pendingOrderWrapper = new LambdaQueryWrapper<>();
        pendingOrderWrapper.eq(Order::getCustomerId, customerId)
                .eq(Order::getStatus, "待支付");
        stats.put("pendingOrderCount", orderMapper.selectCount(pendingOrderWrapper));

        return stats;
    }
}
