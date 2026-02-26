package com.github.product_validation_service.core.service;

import com.github.product_validation_service.config.exception.ValidationException;
import com.github.product_validation_service.core.dto.Event;
import com.github.product_validation_service.core.dto.History;
import com.github.product_validation_service.core.dto.OrderProducts;
import com.github.product_validation_service.core.model.Validation;
import com.github.product_validation_service.core.producer.KafkaProducer;
import com.github.product_validation_service.core.repository.ProductRepository;
import com.github.product_validation_service.core.repository.ValidationRepository;
import com.github.product_validation_service.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.github.product_validation_service.core.enums.ESagaStatus.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProducts(Event event) {
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to validate products: {}", e.getMessage());
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }


    private void checkCurrentValidation(Event event) {
        validateProductsInformed(event);
        if(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another transaction for this validation.");
        }
        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });

    }

    private void createValidation(Event event, boolean success) {
        var validation = Validation
                .builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .success(success)
                .build();
        validationRepository.save(validation);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Products are validated successfully!");
    }

    private static void validateProductsInformed(Event event) {
        if(isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("Products list is empty!");
        }

        if(isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())) {
            throw new ValidationException("OrderID and TransactionID must be informed!");
        }
    }

    private void validateProductInformed(OrderProducts products) {
        if(isEmpty(products.getProduct()) || isEmpty(products.getProduct().getCode())) {
            throw new ValidationException("Product must be informed!");
        }
    }

    private void validateExistingProduct(String code) {
        if(!productRepository.existsByCode(code)) {
            throw new ValidationException("Product does not exists in database");
        }
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to validate Products: ".concat(message));
    }

    public void rollbackEvent(Event event) {
        chageValidationToFail(event);
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed on product validation!");
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void chageValidationToFail(Event event) {
        validationRepository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getPayload().getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(false);
                    validationRepository.save(validation);
                },
                        () -> createValidation(event, false));
    }

}
