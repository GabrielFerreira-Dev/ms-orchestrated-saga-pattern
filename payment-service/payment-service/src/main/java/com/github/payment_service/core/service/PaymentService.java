package com.github.payment_service.core.service;

import com.github.payment_service.config.exception.ValidationException;
import com.github.payment_service.core.dto.Event;
import com.github.payment_service.core.dto.History;
import com.github.payment_service.core.dto.OrderProducts;
import com.github.payment_service.core.enums.EPaymentStatus;
import com.github.payment_service.core.enums.ESagaStatus;
import com.github.payment_service.core.model.Payment;
import com.github.payment_service.core.producer.KafkaProducer;
import com.github.payment_service.core.repository.PaymentRepository;
import com.github.payment_service.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.github.payment_service.core.enums.EPaymentStatus.REFUND;
import static com.github.payment_service.core.enums.ESagaStatus.*;
import static com.github.payment_service.core.enums.ESagaStatus.SUCCESS;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_VALIDATION_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final PaymentRepository paymentRepository;

    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);
            var payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to realize payment: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }


    private void checkCurrentValidation(Event event) {
        if(paymentRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another transaction for this validation.");
        }
    }

    private void createPendingPayment(Event event) {
        var totalAmount = calculateAmount(event);
        var totalItems = calculateItems(event);

        var payment = Payment
                .builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    private double calculateAmount(Event event) {
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private int calculateItems(Event event) {
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private void setEventAmountItems(Event event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    private void validateAmount(double amount) {
        if(amount < MIN_AMOUNT_VALUE) {
            throw new ValidationException("The minimum amount available is ".concat(MIN_AMOUNT_VALUE.toString()));
        }
    }

    private void changePaymentToSuccess(Payment payment) {
        payment.setStatus(EPaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Payment realized successfully!");
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
        addHistory(event, "Fail to realize Payment: ".concat(message));
    }

    public void realizerRefund(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            changePaymentStatusToRefund(event);
            addHistory(event, "Rollback executed for payment!");
        } catch (Exception e) {
            addHistory(event, "Rollback not executed for payment: ".concat(e.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void changePaymentStatusToRefund(Event event) {
        var payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

    private Payment findByOrderIdAndTransactionId(Event event) {
        return paymentRepository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found by OrderID and TransactionID"));
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }
}
