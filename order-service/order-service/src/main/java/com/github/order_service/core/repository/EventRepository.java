package com.github.order_service.core.repository;

import com.github.order_service.core.document.Event;
import com.github.order_service.core.document.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findAllByOrderByCreatedAtDesc();

    Optional<Event> findTop1ByOrderIdOrderByCreatedAtDesc(String orderID);

    Optional<Event> findTop1ByTransactionIdOrderByCreatedAtDesc(String transactionID);
}
