package com.fangyicha.config;

import com.fangyicha.mapper.DeveloperMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final DeveloperMapper developerMapper;
    private final DataSource dataSource;

    public DataInitializer(DeveloperMapper developerMapper, DataSource dataSource) {
        this.developerMapper = developerMapper;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        if (developerMapper.selectCount(null) > 0) {
            log.info("数据库已有数据，跳过初始化");
            return;
        }
        log.info("开始初始化种子数据...");
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/data.sql"));
            populator.setContinueOnError(false);
            populator.execute(dataSource);
            log.info("种子数据初始化完成");
        } catch (Exception e) {
            log.error("种子数据初始化失败", e);
            throw new RuntimeException("种子数据初始化失败", e);
        }
    }
}
