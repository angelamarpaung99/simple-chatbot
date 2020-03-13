package com.bot.emosigw;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.linecorp.bot.model.event.Event;

import java.util.List;
import java.util.Collections;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventsModel {

    private final List<Event> events;

    @JsonCreator
    public EventsModel(@JsonProperty("events") List<Event> events) {
        this.events = events != null ? events : Collections.emptyList();
    }

    public List<Event> getEvents() {
        return events;
    }
}
