package com.fangyicha.config;

import com.fangyicha.entity.PriceHistory;
import com.fangyicha.mapper.DeveloperMapper;
import com.fangyicha.mapper.PriceHistoryMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.PriceHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final DeveloperMapper developerMapper;
    private final DataSource dataSource;
    private final PropertyMapper propertyMapper;
    private final PriceHistoryService priceHistoryService;
    private final PriceHistoryMapper priceHistoryMapper;

    public DataInitializer(DeveloperMapper developerMapper,
                           DataSource dataSource,
                           PropertyMapper propertyMapper,
                           PriceHistoryService priceHistoryService,
                           PriceHistoryMapper priceHistoryMapper) {
        this.developerMapper = developerMapper;
        this.dataSource = dataSource;
        this.propertyMapper = propertyMapper;
        this.priceHistoryService = priceHistoryService;
        this.priceHistoryMapper = priceHistoryMapper;
    }

    @Override
    public void run(String... args) {
        if (developerMapper.selectCount(null) > 0) {
            log.info("数据库已有数据，跳过初始化");
            // 检查是否需要生成价格历史数据
            if (priceHistoryMapper.selectCount(null) == 0) {
                generatePriceHistoryData();
            }
            return;
        }
        log.info("开始初始化种子数据...");
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/data.sql"));
            populator.setContinueOnError(false);
            populator.execute(dataSource);
            log.info("种子数据初始化完成");

            // 生成价格历史数据
            generatePriceHistoryData();
        } catch (Exception e) {
            log.error("种子数据初始化失败", e);
            throw new RuntimeException("种子数据初始化失败", e);
        }
    }

    /**
     * 为所有房产生成24个月价格历史数据
     */
    private void generatePriceHistoryData() {
        log.info("开始生成价格历史数据...");
        try {
            List<com.fangyicha.entity.Property> properties = propertyMapper.selectList(null);
            if (properties.isEmpty()) {
                log.warn("没有房产数据，跳过价格历史生成");
                return;
            }

            Random random = new Random(42); // 固定种子保证可重复
            LocalDate baseDate = LocalDate.of(2026, 5, 1);
            List<PriceHistory> batch = new ArrayList<>();

            for (com.fangyicha.entity.Property property : properties) {
                BigDecimal basePricePerSqm = property.getPricePerSqm() != null
                        ? property.getPricePerSqm()
                        : BigDecimal.valueOf(10000);
                BigDecimal baseTotalPrice = property.getTotalPrice() != null
                        ? property.getTotalPrice()
                        : BigDecimal.valueOf(1000000);

                for (int i = 23; i >= 0; i--) {
                    LocalDate recordDate = baseDate.minusMonths(i);

                    // 模拟价格波动：每月变化在 -3% 到 +3% 之间
                    double fluctuation = 1.0 + (random.nextDouble() - 0.5) * 0.06;
                    // 长期趋势：每月微涨0.15%
                    double trend = 1.0 + 0.0015 * (23 - i);

                    BigDecimal pricePerSqm = basePricePerSqm
                            .multiply(BigDecimal.valueOf(fluctuation))
                            .multiply(BigDecimal.valueOf(trend))
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    BigDecimal totalPrice = baseTotalPrice
                            .multiply(BigDecimal.valueOf(fluctuation))
                            .multiply(BigDecimal.valueOf(trend))
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    PriceHistory history = new PriceHistory();
                    history.setPropertyId(property.getId());
                    history.setRecordDate(recordDate);
                    history.setPricePerSqm(pricePerSqm);
                    history.setTotalPrice(totalPrice);
                    batch.add(history);

                    // 每500条批量插入一次
                    if (batch.size() >= 500) {
                        priceHistoryService.saveBatch(batch);
                        batch.clear();
                    }
                }
            }

            // 插入剩余数据
            if (!batch.isEmpty()) {
                priceHistoryService.saveBatch(batch);
            }

            log.info("价格历史数据生成完成");
        } catch (Exception e) {
            log.error("价格历史数据生成失败", e);
            // 不阻止启动
        }
    }
}
