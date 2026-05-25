package com.fangyicha.config;

import com.fangyicha.FangYiChaApplication;
import com.fangyicha.mapper.DeveloperMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = FangYiChaApplication.class)
class DataInitializerTest {

    @Autowired
    private DeveloperMapper developerMapper;

    @Test
    void testSeedDataLoaded() {
        long count = developerMapper.selectCount(null);
        assertTrue(count > 0, "Seed data should be loaded, developer table should not be empty");
    }
}
