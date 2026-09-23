package com.crd.rental.service;
import com.crd.rental.domain.Car;
import com.crd.rental.domain.CarType;
import com.crd.rental.domain.Reservation;
import com.crd.rental.repository.InMemoryCarRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class ReservationService {
    private final InMemoryCarRepository carRepository;
    private final Map<CarType, ReentrantReadWriteLock> lockStripes = new ConcurrentHashMap<>();

    public ReservationService(InMemoryCarRepository carRepository) {
        this.carRepository = carRepository;
        for (CarType type : CarType.values()) {
            lockStripes.put(type, new ReentrantReadWriteLock());
        }
    }

    public Optional<Reservation> bookCar(CarType type, LocalDateTime start, int rentalDays) {
        if (rentalDays <= 0) throw new IllegalArgumentException("Duration must be >= 1 day");
        LocalDateTime end = start.plusDays(rentalDays);

        ReentrantReadWriteLock lock = lockStripes.get(type);
        lock.writeLock().lock();
        try {
            List<Car> targetedCars = carRepository.findByCarType(type);
            Optional<Car> availableCar = targetedCars.parallelStream()
                    .filter(car -> isTimelineAvailable(carRepository.getScheduleForCar(car.id()), start, end))
                    .findFirst();

            if (availableCar.isPresent()) {
                Car car = availableCar.get();
                carRepository.getScheduleForCar(car.id()).put(start, end);
                return Optional.of(new Reservation(UUID.randomUUID().toString(), car, start, end));
            }
            return Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean isTimelineAvailable(TreeMap<LocalDateTime, LocalDateTime> timeline, LocalDateTime start, LocalDateTime end) {
        LocalDateTime lowerBoundStart = timeline.floorKey(start);
        if (lowerBoundStart != null && timeline.get(lowerBoundStart).isAfter(start)) {
            return false; 
        }
        LocalDateTime upperBoundStart = timeline.ceilingKey(start);
        return upperBoundStart == null || !upperBoundStart.isBefore(end);
    }
}
