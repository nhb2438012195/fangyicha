package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.entity.Suggestion;

/**
 * 建议/购房意向服务接口
 */
public interface SuggestionService extends IService<Suggestion> {

    /**
     * 提交建议
     */
    boolean submitSuggestion(Long customerId, Suggestion suggestion);

    /**
     * 客户查看自己的建议列表
     */
    Page<Suggestion> getMySuggestions(Long customerId, Integer page, Integer pageSize);

    /**
     * 开发商查看收到的建议列表
     */
    Page<Suggestion> getReceivedSuggestions(Long developerId, Integer page, Integer pageSize);

    /**
     * 开发商回复建议
     */
    boolean replySuggestion(Long id, Long developerId, String replyContent);
}
