package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Favorite;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.FavoriteMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Slf4j
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final PropertyMapper propertyMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, PropertyMapper propertyMapper) {
        this.favoriteMapper = favoriteMapper;
        this.propertyMapper = propertyMapper;
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long customerId, Long propertyId) {
        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            throw new IllegalArgumentException("楼盘不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getCustomerId, customerId)
               .eq(Favorite::getPropertyId, propertyId);
        Favorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            log.info("取消收藏: customerId={}, propertyId={}", customerId, propertyId);
            return false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setCustomerId(customerId);
            favorite.setPropertyId(propertyId);
            try {
                favoriteMapper.insert(favorite);
            } catch (DuplicateKeyException e) {
                log.warn("重复收藏尝试: customerId={}, propertyId={}", customerId, propertyId);
                throw new DuplicateKeyException("已收藏该楼盘");
            }
            log.info("添加收藏: customerId={}, propertyId={}", customerId, propertyId);
            return true;
        }
    }

    @Override
    public List<FavoriteDTO> getFavorites(Long customerId) {
        return favoriteMapper.selectFavoritesWithProperty(customerId);
    }

    @Override
    public boolean isFavorited(Long customerId, Long propertyId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getCustomerId, customerId)
               .eq(Favorite::getPropertyId, propertyId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Map<Long, Boolean> getFavoriteStatusBatch(Long customerId, List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getCustomerId, customerId)
               .in(Favorite::getPropertyId, propertyIds);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        Map<Long, Boolean> result = propertyIds.stream().collect(
            Collectors.toMap(id -> id, id -> false)
        );
        for (Favorite fav : favorites) {
            result.put(fav.getPropertyId(), true);
        }
        return result;
    }
}
