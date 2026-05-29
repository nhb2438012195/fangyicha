package com.fangyicha.ai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class CreateOrderPreviewInput {
    @JsonProperty(required = true)
    @JsonPropertyDescription("要购买的楼盘名称，例如「招商·湘江府」")
    private String propertyName;
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
}
