package com.fangyicha.common;

/**
 * 系统常量定义
 */
public interface Constants {

    /** 角色：开发商 */
    String ROLE_DEVELOPER = "ROLE_DEVELOPER";
    /** 角色：客户 */
    String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    /** 房产状态 */
    String PROPERTY_STATUS_ON_SALE = "在售";
    String PROPERTY_STATUS_SOLD = "已售";
    String PROPERTY_STATUS_PENDING = "待开盘";

    /** 装修情况 */
    String DECORATION_SHELL = "毛坯";
    String DECORATION_SIMPLE = "简装";
    String DECORATION_DECENT = "精装";
    String DECORATION_LUXURY = "豪装";

    /** 户型类型 */
    String[] FLOOR_PLAN_TYPES = {"一室一厅", "两室一厅", "三室两厅", "四室两厅", "复式", "别墅"};

    /** 购房紧迫度 */
    String[] URGENCY_OPTIONS = {"一个月内", "三个月内", "半年内", "一年内", "不限"};

    /** 购房意向 */
    String[] INTENTION_OPTIONS = {"自住", "投资", "改善", "学区", "养老"};

    /** 建议状态 */
    String SUGGESTION_PENDING = "待回复";
    String SUGGESTION_REPLIED = "已回复";
    String SUGGESTION_CLOSED = "已关闭";

    /** 订单状态 */
    String ORDER_PENDING_PAYMENT = "待支付";
    String ORDER_PAID = "已支付";
    String ORDER_CANCELLED = "已取消";
    String ORDER_COMPLETED = "已完成";

    /** 订单实体类型 */
    String ENTITY_ORDER = "Order";
    String ENTITY_ORDER_LOG = "OrderLog";

    /** JWT Claims */
    String JWT_CLAIM_USER_ID = "userId";
    String JWT_CLAIM_ROLE = "role";
    String JWT_CLAIM_USERNAME = "username";

    /** 请求头 */
    String AUTH_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";

    /** 默认分页 */
    int DEFAULT_PAGE = 1;
    int DEFAULT_PAGE_SIZE = 10;

}
