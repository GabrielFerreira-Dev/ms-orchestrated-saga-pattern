package com.github.orchestrator_service.core.service;

import com.github.orchestrator_service.core.dto.Event;
import com.github.orchestrator_service.core.dto.History;
import com.github.orchestrator_service.core.enums.ETopics;
import com.github.orchestrator_service.core.producer.SagaOrchestratorProducer;
import com.github.orchestrator_service.core.saga.SagaExecutionController;
import com.github.orchestrator_service.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.github.orchestrator_service.core.enums.EEventSource.ORCHESTRATOR;
import static com.github.orchestrator_service.core.enums.ESagaStatus.FAIL;
import static com.github.orchestrator_service.core.enums.ESagaStatus.SUCCESS;
import static com.github.orchestrator_service.core.enums.ETopics.NOTIFY_ENDING;

@Slf4j
@Service
@AllArgsConstructor
public class OrchestratorService {

    private final JsonUtil jsonUtil;
    private final SagaOrchestratorProducer producer;
    private SagaExecutionController executionController;

    public void startSaga(Event event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCCESS);
        var topic = getTopic(event);
        log.info("SAGA STARTED!");
        addHistory(event, "Saga started!");
        sendToProducerWithTopic(event, topic);
    }

    public void finishSagaSuccess(Event event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCCESS);
        log.info("SAGA FINISHED SUCCESSFULLY FOR EVENT {}!", event.getId());
        addHistory(event, "Saga finished successfully!");
        notifyFinishedSaga(event);
    }

    public void finishSagaFail(Event event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(FAIL);
        log.info("SAGA FINISHED WITH ERRORS FOR EVENT {}!", event.getId());
        addHistory(event, "Saga finished with errors!");
        notifyFinishedSaga(event);
    }

    public void continueSaga(Event event) {
        var topic = getTopic(event);
        log.info("SAGA CONTINUING FOR EVENT {}", event.getId());
        sendToProducerWithTopic(event, topic);
    }

    private void sendToProducerWithTopic(Event event, ETopics topic) {
        producer.sendEvent(jsonUtil.toJson(event), topic.getTopic());
    }

    private ETopics getTopic(Event event) {
        return executionController.getNextTopic(event);
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

    private void notifyFinishedSaga(Event event) {
        sendToProducerWithTopic(event, NOTIFY_ENDING);
    }

}
