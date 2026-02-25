package com.github.order_service.core.controller;

import com.github.order_service.core.document.Event;
import com.github.order_service.core.dto.EventFilter;
import com.github.order_service.core.service.EventService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/event")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public Event findByFilters(EventFilter eventFilter) {
        return eventService.findByFilters(eventFilter);
    }

    @GetMapping("all")
    public List<Event> findAll() {
        return eventService.findAll();
    }
}
