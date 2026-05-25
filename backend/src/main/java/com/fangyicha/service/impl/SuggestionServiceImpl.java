package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.common.Constants;
import com.fangyicha.entity.Developer;
import com.fangyicha.entity.Customer;
import com.fangyicha.entity.Suggestion;
import com.fangyicha.mapper.DeveloperMapper;
import com.fangyicha.mapper.CustomerMapper;
import com.fangyicha.mapper.SuggestionMapper;
import com.fangyicha.service.SuggestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 建议/购房意向服务实现
 */
@Slf4j
@Service
public class SuggestionServiceImpl extends ServiceImpl<SuggestionMapper, Suggestion> implements SuggestionService {

    private final SuggestionMapper suggestionMapper;
    private final DeveloperMapper developerMapper;
    private final CustomerMapper customerMapper;

    public SuggestionServiceImpl(SuggestionMapper suggestionMapper,
                                 DeveloperMapper developerMapper,
                                 CustomerMapper customerMapper) {
        this.suggestionMapper = suggestionMapper;
        this.developerMapper = developerMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional
    public boolean submitSuggestion(Long customerId, Suggestion suggestion) {
        suggestion.setCustomerId(customerId);
        suggestion.setStatus(Constants.SUGGESTION_PENDING);
        int result = suggestionMapper.insert(suggestion);
        log.info("客户{}提交建议给开发商{}", customerId, suggestion.getDeveloperId());
        return result > 0;
    }

    @Override
    public Page<Suggestion> getMySuggestions(Long customerId, Integer page, Integer pageSize) {
        Page<Suggestion> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Suggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Suggestion::getCustomerId, customerId);
        wrapper.orderByDesc(Suggestion::getCreatedTime);

        Page<Suggestion> result = suggestionMapper.selectPage(pageParam, wrapper);

        // 填充关联数据
        for (Suggestion suggestion : result.getRecords()) {
            Developer developer = developerMapper.selectById(suggestion.getDeveloperId());
            if (developer != null) {
                suggestion.setDeveloperName(developer.getCompanyName());
            }
            Customer customer = customerMapper.selectById(suggestion.getCustomerId());
            if (customer != null) {
                suggestion.setCustomerName(customer.getRealName());
            }
        }
        return result;
    }

    @Override
    public Page<Suggestion> getReceivedSuggestions(Long developerId, Integer page, Integer pageSize) {
        Page<Suggestion> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Suggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Suggestion::getDeveloperId, developerId);
        wrapper.orderByDesc(Suggestion::getCreatedTime);

        Page<Suggestion> result = suggestionMapper.selectPage(pageParam, wrapper);

        // 填充关联数据
        for (Suggestion suggestion : result.getRecords()) {
            Developer developer = developerMapper.selectById(suggestion.getDeveloperId());
            if (developer != null) {
                suggestion.setDeveloperName(developer.getCompanyName());
            }
            Customer customer = customerMapper.selectById(suggestion.getCustomerId());
            if (customer != null) {
                suggestion.setCustomerName(customer.getRealName());
            }
        }
        return result;
    }

    @Override
    @Transactional
    public boolean replySuggestion(Long id, Long developerId, String replyContent) {
        Suggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null || !suggestion.getDeveloperId().equals(developerId)) {
            return false;
        }
        suggestion.setReplyContent(replyContent);
        suggestion.setStatus(Constants.SUGGESTION_REPLIED);
        int result = suggestionMapper.updateById(suggestion);
        log.info("开发商{}回复建议{}", developerId, id);
        return result > 0;
    }
}
