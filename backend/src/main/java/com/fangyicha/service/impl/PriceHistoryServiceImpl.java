package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.entity.PriceHistory;
import com.fangyicha.mapper.PriceHistoryMapper;
import com.fangyicha.service.PriceHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 价格历史服务实现
 */
@Slf4j
@Service
public class PriceHistoryServiceImpl extends ServiceImpl<PriceHistoryMapper, PriceHistory> implements PriceHistoryService {

    private final PriceHistoryMapper priceHistoryMapper;

    public PriceHistoryServiceImpl(PriceHistoryMapper priceHistoryMapper) {
        this.priceHistoryMapper = priceHistoryMapper;
    }

    @Override
    public List<PriceHistory> getPriceHistory(Long propertyId, int months) {
        return priceHistoryMapper.selectByPropertyIdAndMonths(propertyId, months);
    }
}
