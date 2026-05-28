package com.fangyicha.service.impl;

import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Customer;
import com.fangyicha.entity.Order;
import com.fangyicha.entity.PendingOrder;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.*;
import com.fangyicha.service.FavoriteService;
import com.fangyicha.service.FunctionCallService;
import com.fangyicha.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FunctionCallServiceImplTest {

    @Mock private FavoriteService favoriteService;
    @Mock private OrderService orderService;
    @Mock private PropertyMapper propertyMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private PendingOrderMapper pendingOrderMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private RagServiceImpl ragService;

    private FunctionCallServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FunctionCallServiceImpl(favoriteService, orderService, propertyMapper,
            customerMapper, pendingOrderMapper, favoriteMapper, ragService);
    }

    @Test
    void testAddFavoriteByPropertyId() {
        Property property = new Property();
        property.setId(1L);
        property.setPropertyName("测试楼盘");
        when(propertyMapper.selectById(1L)).thenReturn(property);
        when(favoriteService.toggleFavorite(100L, 1L)).thenReturn(true);

        var result = service.execute("add_favorite", Map.of("propertyId", 1), 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("已收藏"));
    }

    @Test
    void testAddFavoritePropertyNotFound() {
        var result = service.execute("add_favorite", Map.of("propertyName", "不存在楼盘"), 100L);
        assertFalse(result.isSuccess());
    }

    @Test
    void testViewFavoritesWithData() {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setPropertyId(1L);
        dto.setPropertyName("收藏楼盘");
        dto.setLocation("测试位置");
        when(favoriteService.getFavorites(100L)).thenReturn(List.of(dto));

        var result = service.execute("view_favorites", Map.of(), 100L);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testViewFavoritesEmpty() {
        when(favoriteService.getFavorites(100L)).thenReturn(Collections.emptyList());

        var result = service.execute("view_favorites", Map.of(), 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("还没有收藏"));
    }

    @Test
    void testCreateOrderPreview() {
        Property property = new Property();
        property.setId(3L);
        property.setPropertyName("测试楼盘");
        property.setTotalPrice(new BigDecimal("50000000"));
        property.setPricePerSqm(new BigDecimal("8000"));
        property.setAreaSqm(new BigDecimal("62.5"));
        property.setFloorPlanType("三室两厅");
        property.setLocation("测试位置");
        property.setStatus("在售");

        Customer customer = new Customer();
        customer.setId(100L);
        customer.setRealName("张三");
        customer.setPhone("13800138000");

        when(propertyMapper.selectById(3L)).thenReturn(property);
        when(customerMapper.selectById(100L)).thenReturn(customer);

        var result = service.execute("create_order_preview", Map.of("propertyId", 3), 100L);
        assertTrue(result.isSuccess());
        assertEquals("测试楼盘", result.getData().get("propertyName"));
    }

    @Test
    void testCreateOrderPreviewNotOnSale() {
        Property property = new Property();
        property.setId(3L);
        property.setPropertyName("已售楼盘");
        property.setStatus("已售");

        when(propertyMapper.selectById(3L)).thenReturn(property);

        var result = service.execute("create_order_preview", Map.of("propertyId", 3), 100L);
        assertFalse(result.isSuccess());
    }

    @Test
    void testConfirmOrderWithPending() {
        PendingOrder pending = new PendingOrder();
        pending.setId(1L);
        pending.setPropertyId(3L);
        pending.setCustomerId(100L);
        pending.setStatus("pending");

        Order order = new Order();
        order.setId(10L);
        order.setOrderNo("ORD-20260528");

        doReturn(pending).when(pendingOrderMapper).selectOne(any());
        when(orderService.createOrder(100L, 3L)).thenReturn(order);

        var result = service.execute("confirm_order", Map.of("_sessionId", 5L), 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("ORD-20260528"));
    }

    @Test
    void testConfirmOrderWithoutPending() {
        doReturn(null).when(pendingOrderMapper).selectOne(any());

        var result = service.execute("confirm_order", Map.of("_sessionId", 5L), 100L);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("没有待确认"));
    }

    @Test
    void testUnknownFunction() {
        var result = service.execute("unknown_func", Map.of(), 100L);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("未知函数"));
    }
}
