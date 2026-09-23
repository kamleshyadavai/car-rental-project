package com.crd.rental.repository;
import com.crd.rental.domain.Car;
import com.crd.rental.domain.CarType;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryCarRepository {
    private final Map<CarType, List<Car>> carsByType = new ConcurrentHashMap<>();
    private final Map<String, TreeMap<LocalDateTime, LocalDateTime>> scheduleIndices = new ConcurrentHashMap<>();

    public void saveCar(Car car) {
        carsByType.computeIfAbsent(car.type(), k -> new CopyOnWriteArrayList<>()).add(car);
        scheduleIndices.putIfAbsent(car.id(), new TreeMap<>());
    }

    public List<Car> findByCarType(CarType type) {
        return carsByType.getOrDefault(type, Collections.emptyList());
    }

    public TreeMap<LocalDateTime, LocalDateTime> getScheduleForCar(String carId) {
        return scheduleIndices.get(carId);
    }
    
    public Collection<Car> findAll() {
        List<Car> allCars = new ArrayList<>();
        carsByType.values().forEach(allCars::addAll);
        return allCars;
    }
}
