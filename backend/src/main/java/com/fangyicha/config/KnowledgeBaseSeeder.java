package com.fangyicha.config;

import com.fangyicha.entity.Developer;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.DeveloperMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 知识库数据播种器
 * 应用启动时将平台数据（楼盘+开发商）导出为文本文件，然后重建索引
 */
@Slf4j
@Component
public class KnowledgeBaseSeeder {

    private static final String PLATFORM_DIR = "data/rag-platform";

    private final PropertyMapper propertyMapper;
    private final DeveloperMapper developerMapper;
    private final RagService ragService;

    public KnowledgeBaseSeeder(PropertyMapper propertyMapper,
                                DeveloperMapper developerMapper,
                                RagService ragService) {
        this.propertyMapper = propertyMapper;
        this.developerMapper = developerMapper;
        this.ragService = ragService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("开始播种知识库平台数据...");
            Files.createDirectories(Path.of(PLATFORM_DIR));

            // Export properties
            List<Property> properties = propertyMapper.selectList(null);
            for (Property p : properties) {
                String developerName = getDeveloperName(p.getDeveloperId());
                String content = String.format(
                    "楼盘名称：%s，位置：%s，户型：%s，面积：%s平米，价格：%s万元，开发商：%s，描述：%s",
                    p.getPropertyName() != null ? p.getPropertyName() : "",
                    p.getLocation() != null ? p.getLocation() : "",
                    p.getFloorPlanType() != null ? p.getFloorPlanType() : "",
                    p.getAreaSqm() != null ? p.getAreaSqm().stripTrailingZeros().toPlainString() : "",
                    p.getTotalPrice() != null ? p.getTotalPrice().divide(java.math.BigDecimal.valueOf(10000)).stripTrailingZeros().toPlainString() : "",
                    developerName,
                    p.getDescription() != null ? p.getDescription() : ""
                );
                String filename = "property_" + p.getId() + ".txt";
                Files.writeString(Path.of(PLATFORM_DIR, filename), content, StandardCharsets.UTF_8);
                log.debug("导出楼盘数据: {}", filename);
            }

            // Export developers
            List<Developer> developers = developerMapper.selectList(null);
            for (Developer d : developers) {
                String content = String.format(
                    "开发商：%s，所在地：%s，简介：%s",
                    d.getCompanyName() != null ? d.getCompanyName() : "",
                    d.getAddress() != null ? d.getAddress() : "",
                    d.getDescription() != null ? d.getDescription() : ""
                );
                String filename = "developer_" + d.getId() + ".txt";
                Files.writeString(Path.of(PLATFORM_DIR, filename), content, StandardCharsets.UTF_8);
                log.debug("导出开发商数据: {}", filename);
            }

            log.info("知识库平台数据导出完成: {}个楼盘, {}个开发商", properties.size(), developers.size());

            // Rebuild Lucene index
            ragService.rebuildIndex();
            log.info("知识库索引重建完成");

        } catch (IOException e) {
            log.error("知识库数据播种失败", e);
        }
    }

    /**
     * 获取开发商名称
     */
    private String getDeveloperName(Long developerId) {
        if (developerId == null) return "";
        Developer d = developerMapper.selectById(developerId);
        return d != null ? d.getCompanyName() : "";
    }
}
