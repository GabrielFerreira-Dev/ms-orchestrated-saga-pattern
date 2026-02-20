package com.github.inventory_service.core.dto;

import com.github.inventory_service.core.enums.EEventSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    private String id;
    private String transacionId;
    private String orderId;
    private Order payload;
    private EEventSource source;
    private String status;
    private List<History> eventHistory;
    private LocalDateTime createdAt;

}
