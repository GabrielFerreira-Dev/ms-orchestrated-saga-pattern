package com.github.order_service.core.service;

import com.github.order_service.core.document.Event;
import com.github.order_service.core.document.Order;
import com.github.order_service.core.dto.OrderRequest;
import com.github.order_service.core.producer.SagaProducer;
import com.github.order_service.core.repository.EventRepository;
import com.github.order_service.core.repository.OrderRepository;
import com.github.order_service.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    private final OrderRepository repository;
    private final SagaProducer producer;
    private final JsonUtil jsonUtil;
    private final EventService eventService;

    public Order createOrder(OrderRequest orderRequest) {
        var order = Order
                .builder()
                .products(orderRequest.getProducts())
                .createdAt(LocalDateTime.now())
                .transactionId(
                        String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID())
                )
                .build();
        repository.save(order);
        producer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }

    public Event createPayload(Order order) {
        var event = Event
                .builder()
                .orderId(order.getId())
                .transactionId(order.getTransactionId())
                .payload(order)
                .createdAt(LocalDateTime.now())
                .build();

        eventService.save(event);
        return event;
    }
}
