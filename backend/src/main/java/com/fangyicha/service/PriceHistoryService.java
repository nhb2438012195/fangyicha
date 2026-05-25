package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.entity.PriceHistory;

import java.util.List;

/**
 * 价格历史服务接口
 */
public interface PriceHistoryService extends IService<PriceHistory> {

    /**
     * 获取指定房产近24个月的价格历史
     */
    List<PriceHistory> getPriceHistory(Long propertyId, int months);
}
