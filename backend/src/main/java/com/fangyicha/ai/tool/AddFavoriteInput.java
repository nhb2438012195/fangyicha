package com.fangyicha.ai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class AddFavoriteInput {
    @JsonProperty(required = true)
    @JsonPropertyDescription("要收藏的楼盘名称，例如「美的·蓝溪谷」")
    private String propertyName;
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
}
