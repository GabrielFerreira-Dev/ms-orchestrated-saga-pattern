package com.github.inventory_service.core.dto;

import com.github.inventory_service.core.enums.EEventSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class History {

    private EEventSource source;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
