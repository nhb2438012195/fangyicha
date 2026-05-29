package com.fangyicha.ai.tool;

public class ViewFavoritesInput {
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("可选筛选条件，如「朝阳区」「300万以内」")
    private String filter;
    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
}
