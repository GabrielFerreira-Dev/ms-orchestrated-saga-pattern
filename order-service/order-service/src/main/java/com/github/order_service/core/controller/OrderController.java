package com.github.order_service.core.controller;

import com.github.order_service.core.document.Order;
import com.github.order_service.core.dto.OrderRequest;
import com.github.order_service.core.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("api/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Order createOrder(
            @RequestBody OrderRequest orderRequest
            ) {
        return orderService.createOrder(orderRequest);
    }
}
