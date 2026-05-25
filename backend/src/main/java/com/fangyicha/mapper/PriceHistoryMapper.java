package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.entity.PriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 价格历史数据访问层
 */
@Mapper
public interface PriceHistoryMapper extends BaseMapper<PriceHistory> {

    /**
     * 查询指定房产近N个月的价格历史
     */
    @Select("SELECT * FROM price_history " +
            "WHERE property_id = #{propertyId} " +
            "ORDER BY record_date DESC " +
            "LIMIT #{months}")
    List<PriceHistory> selectByPropertyIdAndMonths(@Param("propertyId") Long propertyId,
                                                    @Param("months") int months);
}
