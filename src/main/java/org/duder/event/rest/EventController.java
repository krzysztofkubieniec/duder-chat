package org.duder.event.rest;

import org.duder.chat.websocket.WebSocketEventListener;
import org.duder.dto.event.CreateEvent;
import org.duder.dto.event.EventLoadingMode;
import org.duder.dto.event.EventPreview;
import org.duder.chat.exception.DataNotFoundException;
import org.duder.event.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/event")
class EventController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final EventService eventService;

    EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping()
    public List<EventPreview> findAll(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) EventLoadingMode mode,
                                      @RequestHeader("Authorization") String sessionToken) {
        if (mode == null) {
            mode = EventLoadingMode.PUBLIC;
        }
        return eventService.findAllUnfinished(page, size, mode, sessionToken);
    }

    @PostMapping()
    public ResponseEntity<Void> create(@RequestBody CreateEvent createEvent, @RequestHeader("Authorization") String sessionToken) {
        logger.info("Received create event request " + createEvent + " with sessionToken = " + sessionToken);
        Long eventId = eventService.create(createEvent, sessionToken);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(eventId).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    public EventPreview getEvent(@PathVariable Long id) {
        return eventService.findEvent(id)
                .orElseThrow(() -> new DataNotFoundException("No event found with id " + id));
    }
}
