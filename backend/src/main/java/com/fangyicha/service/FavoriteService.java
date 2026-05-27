package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Favorite;

import java.util.List;
import java.util.Map;

/**
 * 收藏服务接口
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 切换收藏状态
     * 如果已收藏则取消收藏，如果未收藏则添加收藏
     *
     * @param customerId 当前登录客户ID
     * @param propertyId 目标楼盘ID
     * @return true=收藏成功, false=取消收藏成功
     * @throws IllegalArgumentException 楼盘不存在时抛出
     */
    boolean toggleFavorite(Long customerId, Long propertyId);

    /**
     * 获取用户的收藏列表（带楼盘信息）
     */
    List<FavoriteDTO> getFavorites(Long customerId);

    /**
     * 判断某楼盘是否已被当前用户收藏
     */
    boolean isFavorited(Long customerId, Long propertyId);

    /**
     * 批量查询多个楼盘的收藏状态
     */
    Map<Long, Boolean> getFavoriteStatusBatch(Long customerId, List<Long> propertyIds);
}
