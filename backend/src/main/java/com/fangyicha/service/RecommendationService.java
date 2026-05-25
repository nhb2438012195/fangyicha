package com.fangyicha.service;

import com.fangyicha.dto.RecommendationDTO;

import java.util.List;

/**
 * 房产推荐服务接口
 */
public interface RecommendationService {

    /**
     * 根据客户偏好获取房产推荐
     * 基于 preferredLocations 匹配 + 预算排序
     */
    List<RecommendationDTO> getRecommendations(Long customerId);
}
