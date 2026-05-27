package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏数据访问层
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询用户的收藏列表（含楼盘信息）
     * 使用 LEFT JOIN 关联 property 表获取楼盘详情
     * 按收藏时间倒序排列
     */
    @Select("""
        SELECT
            f.id AS favoriteId,
            f.created_time AS createdTime,
            p.id AS propertyId,
            p.property_name AS propertyName,
            p.location,
            p.price_per_sqm AS pricePerSqm,
            p.total_price AS totalPrice,
            p.area_sqm AS areaSqm,
            p.floor_plan_type AS floorPlanType,
            p.decoration,
            SUBSTRING_INDEX(p.image_urls, ',', 1) AS imageUrl,
            d.company_name AS developerName
        FROM favorite f
        LEFT JOIN property p ON f.property_id = p.id
        LEFT JOIN developer d ON p.developer_id = d.id
        WHERE f.customer_id = #{customerId}
        ORDER BY f.created_time DESC
    """)
    List<FavoriteDTO> selectFavoritesWithProperty(@Param("customerId") Long customerId);
}
