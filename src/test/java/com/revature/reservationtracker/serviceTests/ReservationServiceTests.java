package com.revature.reservationtracker.serviceTests;

import com.revature.entities.Reservation;
import com.revature.entities.Room;
import com.revature.entities.User;
import com.revature.repos.ReservationRepo;
import com.revature.repos.RoomRepo;
import com.revature.services.ReservationService;
import com.revature.services.ReservationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
//import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;


@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ReservationServiceTests {
    @MockBean
    private ReservationRepo reservationRepo;

    @MockBean
    private RoomRepo roomRepo;

    @InjectMocks
    private ReservationServiceImpl service;

    static User user = new User(1, "john.doe@revature.com", "trainer");

    @BeforeAll()
    static void setup(){
    }

    @Test
    void create_reservation() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Reservation reservation = new Reservation(505,user.getEmail(),startTime,endTime,"jdjdjdjdjd",0);

        Mockito.when(roomRepo.findById(0)).thenReturn(Optional.of(new Room()));
        Reservation newReservation = service.createReservation(reservation);
        Assertions.assertEquals(0, newReservation.getReservationId());
        Assertions.assertEquals("reserved", newReservation.getStatus());
    }

    @Test
    void create_reservation_invalid_times() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime - 3600;
        Reservation reservation = new Reservation(505,user.getEmail(),startTime,endTime,"jdjdjdjdjd",0);

        Mockito.when(roomRepo.findById(0)).thenReturn(Optional.of(new Room()));
        Assertions.assertThrows(ResponseStatusException.class, ()-> service.createReservation(reservation));
    }

    @Test
    void create_reservation_invalid_room() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime - 3600;
        Reservation reservation = new Reservation(505,user.getEmail(),startTime,endTime,"jdjdjdjdjd",1);

        Mockito.when(roomRepo.findById(1)).thenReturn(Optional.empty());
        Assertions.assertThrows(ResponseStatusException.class, ()-> service.createReservation(reservation));
    }

    @Test
    void update_reservation_time() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Reservation reservation = new Reservation(1,user.getEmail(),startTime,endTime,"reserved",2);

        Mockito.when(reservationRepo.findById(anyInt())).thenReturn(Optional.of(reservation));

        Reservation update = new Reservation(1,user.getEmail(),startTime,endTime,"canceled",4);
        update.setStartTime(startTime + 3600);
        update.setEndTime(endTime + 3600);

        Reservation updated = service.updateReservationTime(update, user);
        Assertions.assertEquals(startTime + 3600, updated.getStartTime());
        Assertions.assertEquals(endTime + 3600, updated.getEndTime());
        Assertions.assertEquals(2, updated.getRoomId());
        Assertions.assertEquals("reserved", updated.getStatus());
    }

    @Test
    void updated_reservation_time_invalid_times(){
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Reservation reservation = new Reservation(1,user.getEmail(),startTime,endTime,"reserved",2);

        Mockito.when(reservationRepo.findById(anyInt())).thenReturn(Optional.of(reservation));

        Reservation update = new Reservation(1,user.getEmail(),startTime,endTime,"canceled",4);
        update.setStartTime(startTime + 3600);
        update.setEndTime(endTime - 3600);

        //service.updateReservationTime(update, user);

        Assertions.assertThrows(ResponseStatusException.class, ()-> service.updateReservationTime(update, user));
    }

    @Test
    void updated_reservation_time_not_found() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Reservation reservation = new Reservation(1,user.getEmail(),startTime,endTime,"reserved",2);

        Mockito.when(reservationRepo.findById(anyInt())).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class, ()-> service.updateReservationTime(reservation, user));
    }

    @Test
    void cancel_reservation(){
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Reservation reservation = new Reservation(1,user.getEmail(),startTime,endTime,"reserved",2);

        Mockito.when(reservationRepo.findById(anyInt())).thenReturn(Optional.of(reservation));

        Reservation cancelledReservation = service.cancelReservation(1, user);

        Assertions.assertEquals("cancelled", cancelledReservation.getStatus());
    }

    @Test
    void get_reservation_by_id() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Reservation reservation = new Reservation(1,user.getEmail(),startTime,endTime,"reserved",2);

        Mockito.when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));

        Reservation gotten = service.getReservationById(1);
        Assertions.assertEquals(gotten.getStartTime(), reservation.getStartTime());
        Assertions.assertEquals(gotten.getEndTime(), reservation.getEndTime());
        Assertions.assertEquals(gotten.getRoomId(), reservation.getRoomId());
    }

    @Test
    void get_reservation_by_id_not_found() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        Mockito.when(reservationRepo.findById(anyInt())).thenReturn(Optional.empty());
        Assertions.assertThrows(ResponseStatusException.class, ()-> service.getReservationById(2));
    }

    @Test
    void cancel_reservation_not_found() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        Mockito.when(reservationRepo.findById(anyInt())).thenReturn(Optional.empty());
        Assertions.assertThrows(ResponseStatusException.class, ()-> service.cancelReservation(2, user));
    }

    @Test
    void get_active_reservations_by_room_id() {
        Mockito.when(reservationRepo.save(any())).then(returnsFirstArg());
        long startTime = System.currentTimeMillis()/1000L;
        long endTime = startTime + 3600;
        Set<Reservation> reservations = new HashSet<>();

        Reservation reservation1 = new Reservation(1,user.getEmail(),startTime,endTime,"reserved",2);
        Reservation reservation2 = new Reservation(2,user.getEmail(),startTime+3600,endTime+3600,"reserved",2);
        Reservation reservation3 = new Reservation(3,user.getEmail(),startTime+7200,endTime+7200,"reserved",2);

        reservations.add(reservation1);
        reservations.add(reservation2);
        reservations.add(reservation3);

        Mockito.when(reservationRepo.findByRoomIdAndStatus(2, "reserved")).thenReturn(reservations);

        Set<Reservation> returned = service.getActiveReservationsByRoomId(2);

        Assertions.assertEquals(3, returned.size());
    }
}