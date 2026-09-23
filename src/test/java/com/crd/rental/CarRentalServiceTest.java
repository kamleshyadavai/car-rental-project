package com.crd.rental;
import com.crd.rental.domain.Car;
import com.crd.rental.domain.CarType;
import com.crd.rental.domain.Reservation;
import com.crd.rental.repository.InMemoryCarRepository;
import com.crd.rental.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class CarRentalServiceTest {
    private InMemoryCarRepository carRepository;
    private ReservationService reservationService;

    @BeforeEach
    public void setUp() {
        carRepository = new InMemoryCarRepository();
        reservationService = new ReservationService(carRepository);
        carRepository.saveCar(new Car("SEDAN-01", CarType.SEDAN));
        carRepository.saveCar(new Car("SEDAN-02", CarType.SEDAN));
        carRepository.saveCar(new Car("SUV-01", CarType.SUV));
        carRepository.saveCar(new Car("VAN-01", CarType.VAN));
    }

    @Test
    public void testSuccessfulReservation() {
        LocalDateTime pickUpTime = LocalDateTime.of(2026, 10, 1, 10, 0);
        Optional<Reservation> reservation = reservationService.bookCar(CarType.SEDAN, pickUpTime, 3);
        assertTrue(reservation.isPresent());
        assertEquals(CarType.SEDAN, reservation.get().car().type());
        assertEquals(pickUpTime, reservation.get().startTime());
    }

    @Test
    public void testInventoryLimitEnforced() {
        LocalDateTime pickUpTime = LocalDateTime.of(2026, 10, 1, 10, 0);
        Optional<Reservation> res1 = reservationService.bookCar(CarType.VAN, pickUpTime, 3);
        Optional<Reservation> res2 = reservationService.bookCar(CarType.VAN, pickUpTime, 2);
        assertTrue(res1.isPresent());
        assertFalse(res2.isPresent());
    }

    @Test
    public void testConsecutiveReservationsPermitted() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 12, 0);
        Optional<Reservation> res1 = reservationService.bookCar(CarType.VAN, start, 4);
        Optional<Reservation> res2 = reservationService.bookCar(CarType.VAN, start.plusDays(4), 2);
        assertTrue(res1.isPresent());
        assertTrue(res2.isPresent());
    }

    @Test
    public void testOverlappingReservationsRejected() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 5, 0, 0);
        reservationService.bookCar(CarType.VAN, start, 5); 
        Optional<Reservation> overlappingRes = reservationService.bookCar(CarType.VAN, start.plusDays(2), 2);
        assertFalse(overlappingRes.isPresent());
    }

    @Test
    public void testConcurrentStressOverlappingRequests() throws InterruptedException, ExecutionException {
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Optional<Reservation>>> tasks = new ArrayList<>();
        LocalDateTime targetTime = LocalDateTime.of(2026, 12, 25, 10, 0);
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> reservationService.bookCar(CarType.SUV, targetTime, 5));
        }
        List<Future<Optional<Reservation>>> results = executor.invokeAll(tasks);
        executor.shutdown();
        int successfulBookings = 0;
        for (Future<Optional<Reservation>> res : results) {
            if (res.get().isPresent()) successfulBookings++;
        }
        assertEquals(1, successfulBookings);
    }
}
