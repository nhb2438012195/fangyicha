package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.Property;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 房产数据访问层
 */
@Mapper
public interface PropertyMapper extends BaseMapper<Property> {

    /**
     * 按位置统计空置率（用于柱状图）
     * 按位置分组，计算平均空置率
     */
    @Select("SELECT p.location as name, AVG(p.vacancy_rate) as vacancyRate, SUM(p.total_units) as totalUnits " +
            "FROM property p WHERE p.developer_id = #{developerId} " +
            "GROUP BY p.location ORDER BY vacancyRate DESC")
    List<Map<String, Object>> selectVacancyByLocation(@Param("developerId") Long developerId);

    /**
     * 按户型统计空置率（用于分组柱状图）
     */
    @Select("SELECT p.floor_plan_type as name, AVG(p.vacancy_rate) as vacancyRate, " +
            "COUNT(*) as propertyCount, SUM(p.total_units) as totalUnits " +
            "FROM property p WHERE p.developer_id = #{developerId} " +
            "GROUP BY p.floor_plan_type ORDER BY vacancyRate DESC")
    List<Map<String, Object>> selectVacancyByType(@Param("developerId") Long developerId);

    /**
     * 楼层与空置率散点数据（用于散点图）
     */
    @Select("SELECT ((p.floor_min + p.floor_max) / 2) as avgFloor, p.vacancy_rate as vacancyRate, " +
            "p.property_name as propertyName, p.floor_plan_type as floorPlanType " +
            "FROM property p WHERE p.developer_id = #{developerId} " +
            "ORDER BY avgFloor ASC")
    List<Map<String, Object>> selectVacancyByFloor(@Param("developerId") Long developerId);
}
