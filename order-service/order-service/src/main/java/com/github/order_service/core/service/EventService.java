package com.github.order_service.core.service;

import com.github.order_service.config.exception.ValidationException;
import com.github.order_service.core.document.Event;
import com.github.order_service.core.dto.EventFilter;
import com.github.order_service.core.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

    private final EventRepository repository;

    public Event save(Event event) {
        return repository.save(event);
    }

    public void notifyEnd(Event event) {
        event.setOrderId(event.getOrderId());
        event.setCreatedAt(event.getCreatedAt());
        save(event);
        log.info("Order {} with SAGA notified! TrasactionId: {}", event.getOrderId(), event.getTransactionId());
    }

    public List<Event> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Event findByFilters(EventFilter eventFilter) {
        validateEmptyFilters(eventFilter);
        if(!ObjectUtils.isEmpty(eventFilter.getOrderId())) {
            return findByOrderId(eventFilter.getOrderId());
        } else  {
            return findByTransactionId(eventFilter.getTransactionId());
        }
    }

    private void validateEmptyFilters(EventFilter eventFilter) {
        if(ObjectUtils.isEmpty(eventFilter.getOrderId()) && ObjectUtils.isEmpty(eventFilter.getTransactionId())) {
            throw new ValidationException("OrderID or TransactionID must be informed.");
        }
    }

    private Event findByOrderId(String orderId) {
        return repository
                .findTop1ByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ValidationException("Event not found by orderID."));
    }

    private Event findByTransactionId(String transactionId) {
        return repository
                .findTop1ByTransactionIdOrderByCreatedAtDesc(transactionId)
                .orElseThrow(() -> new ValidationException("Event not found by transactionID."));
    }
}
