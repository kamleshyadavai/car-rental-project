package com.crd.rental.domain;
import java.time.LocalDateTime;

public record Reservation(String id, Car car, LocalDateTime startTime, LocalDateTime endTime) {
    public boolean isOverlapping(LocalDateTime start, LocalDateTime end) {
        return start.isBefore(this.endTime) && end.isAfter(this.startTime);
    }
}
