package itm.oss.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {

    private Room room(String id, int capacity) {
        return new Room(id, "Test room " + id, capacity, Set.of());
    }

    private Booking booking(String roomId, String start, String end, BookingStatus status) {
        return new Booking(1, "test", roomId, sToTime(start),
                sToTime(end), 2, "Study", status);
    }

    private LocalDateTime sToTime(String time) {
        return LocalDateTime.parse(time);
    }

    @Test
    @DisplayName("Check room availability by capacity and time")
    void returnAvailableRoom() {
        List<Room> rooms = List.of(room("r1", 4), room("r2", 2),
                room("r3", 6));

        List<Booking> bookings = List.of(booking("r3", "2026-10-18T09:00", "2026-10-18T10:30", BookingStatus.ACTIVE));

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, sToTime("2026-10-18T10:00"),
                sToTime("2026-10-18T11:00"), 4);

        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).getId());
    }

    @Test
    @DisplayName("Check overlapping and touching booking handling")
    void checkOverlapLogic() {
        List<Room> rooms = List.of(room("r1", 4), room("r2", 6));

        List<Booking> bookings = List.of(booking("r1", "2026-10-18T09:00", "2026-10-18T10:00", BookingStatus.ACTIVE),
                booking("r2", "2026-10-18T11:00", "2026-10-18T12:00", BookingStatus.ACTIVE));
        // Request touches r1's end and r2's start, so neither booking overlaps
        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, sToTime("2026-10-18T10:00"),
                sToTime("2026-10-18T11:00"), 4);

        assertEquals(2, result.size());
        assertEquals("r1", result.get(0).getId());
        assertEquals("r2", result.get(1).getId());
    }

    @Test
    @DisplayName("Check when result is empty")
    void checkIfNoResult() {
        List<Room> rooms = List.of(room("r1", 2), room("r2", 6));

        List<Booking> bookings = List.of(booking("r1", "2026-10-18T09:00", "2026-10-18T10:00", BookingStatus.ACTIVE),
                booking("r2", "2026-10-18T11:00", "2026-10-18T12:00", BookingStatus.ACTIVE));
        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, sToTime("2026-10-18T10:00"),
                sToTime("2026-10-18T11:30"), 4);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Check cancelled booking does not block room")
    void checkCancelled() {
        List<Room> rooms = List.of(room("r1", 6));

        List<Booking> bookings = List.of(booking("r1", "2026-10-18T09:00",
                "2026-10-18T10:00", BookingStatus.CANCELLED));

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, sToTime("2026-10-18T09:00"),
                sToTime("2026-10-18T11:00"), 4);

        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).getId());
    }

    @Test
    @DisplayName("Check if output preserves input order")
    void checkOrder() {
        List<Room> rooms = List.of(room("r2", 6), room("r3", 4), room("r1", 4));

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, List.of(), sToTime("2026-10-18T10:00"),
                sToTime("2026-10-18T11:00"), 4);

        assertEquals(3, result.size());
        assertEquals("r2", result.get(0).getId());
        assertEquals("r3", result.get(1).getId());
        assertEquals("r1", result.get(2).getId());
    }

    @Test
    void returnsRoomWhenNoBookings() {
        Room room = new Room("R101", "Room A", 40, Set.of("JJB"));
        List<Room> rooms = List.of(room);
        List<Booking> bookings = List.of();
        LocalDateTime start = LocalDateTime.parse("2026-10-20T10:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-10-20T12:00:00");
        int minCapacity = 30;

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, start, end, minCapacity);

        assertEquals(1, result.size(), "Expected exactly one available room");
        assertEquals(room, result.get(0), "Expected the returned room to match the requested available room");
    }

    @Test
    void excludesRoomWithInsufficientCapacity() {
        Room room = new Room("R102", "Room B", 20, Set.of());
        List<Room> rooms = List.of(room);
        List<Booking> bookings = List.of();
        LocalDateTime start = LocalDateTime.parse("2026-10-20T10:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-10-20T12:00:00");
        int minCapacity = 30;

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, start, end, minCapacity);

        assertEquals(0, result.size(), "Rooms with insufficient capacity must be excluded");
    }

    @Test
    void filterRoomsByCapacityAndTime() {
        Room roomA = new Room("R101", "Room A", 40, Set.of());
        Room roomB = new Room("R102", "Room B", 20, Set.of());
        Room roomC = new Room("R103", "Room C", 50, Set.of());
        List<Room> rooms = List.of(roomA, roomB, roomC);
        Booking bookingA = new Booking(1, "s1", "R101", LocalDateTime.parse("2026-10-20T10:00"),
                LocalDateTime.parse("2026-10-20T11:00"), 2, "Study", BookingStatus.ACTIVE);
        Booking bookingC = new Booking(3, "s3", "R103", LocalDateTime.parse("2026-10-20T13:00"),
                LocalDateTime.parse("2026-10-20T15:00"), 2, "Study", BookingStatus.ACTIVE);
        List<Booking> bookings = List.of(bookingA, bookingC);
        LocalDateTime start = LocalDateTime.parse("2026-10-20T10:00");
        LocalDateTime end = LocalDateTime.parse("2026-10-20T12:00");
        int minCapacity = 30;

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings, start, end, minCapacity);

        assertEquals(1, result.size(), "Only one room should satisfy both capacity and time conditions");
        assertEquals(roomC, result.get(0), "Expected the available room to be R103");
    }

    @Test
    void throwsExceptionForInvalidCapacity() {
        Room room = new Room("R103", "Room C", 40, Set.of());
        List<Room> rooms = List.of(room);
        List<Booking> bookings = List.of();
        LocalDateTime start = LocalDateTime.parse("2026-10-20T10:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-10-20T12:00:00");
        int minCapacity = 0;

        assertThrows(IllegalArgumentException.class,
                () -> AvailabilityService.findAvailableRooms(rooms, bookings, start, end, minCapacity),
                "Invalid minimum capacity must throw IllegalArgumentException");
    }

    // ---- Additional tests ----

    @Test
    @DisplayName("Invalid input (null, start >= end, negative capacity) throws IllegalArgumentException")
    void throwsExceptionForInvalidInputs() {
        List<Room> rooms = List.of(room("r1", 4));
        LocalDateTime t = sToTime("2026-10-20T10:00");
        LocalDateTime earlier = sToTime("2026-10-20T09:00");
        LocalDateTime later = sToTime("2026-10-20T11:00");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AvailabilityService.findAvailableRooms(rooms, List.of(), null, later, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AvailabilityService.findAvailableRooms(rooms, List.of(), t, null, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AvailabilityService.findAvailableRooms(rooms, List.of(), t, t, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AvailabilityService.findAvailableRooms(rooms, List.of(), t, earlier, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> AvailabilityService.findAvailableRooms(rooms, List.of(), t, later, -1)));
    }

    @Test
    @DisplayName("Capacity equal to minCapacity is included; smaller room is excluded even if free")
    void includesRoomWithExactCapacity() {
        List<Room> rooms = List.of(room("r1", 4), room("r2", 3));

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, List.of(),
                sToTime("2026-10-20T10:00"), sToTime("2026-10-20T11:00"), 4);

        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).getId());
    }

    @Test
    @DisplayName("Inside, containing, and end-overlapping bookings all block; free room is returned")
    void overlappingPatternsBlockRooms() {
        List<Room> rooms = List.of(room("r1", 6), room("r2", 6), room("r3", 6), room("r4", 6));
        List<Booking> bookings = List.of(
                booking("r1", "2026-10-20T10:15", "2026-10-20T10:45", BookingStatus.ACTIVE), // inside request
                booking("r2", "2026-10-20T09:00", "2026-10-20T13:00", BookingStatus.ACTIVE), // contains request
                booking("r3", "2026-10-20T10:30", "2026-10-20T12:00", BookingStatus.ACTIVE)); // overlaps end

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings,
                sToTime("2026-10-20T10:00"), sToTime("2026-10-20T11:00"), 4);

        assertEquals(1, result.size());
        assertEquals("r4", result.get(0).getId());
    }

    @Test
    @DisplayName("Other room's booking is ignored; cancelled is ignored but active on same room blocks")
    void bookingsAreMatchedByRoomAndStatus() {
        List<Room> rooms = List.of(room("r1", 6), room("r2", 6), room("r3", 6));
        List<Booking> bookings = List.of(
                booking("r1", "2026-10-20T10:00", "2026-10-20T11:00", BookingStatus.ACTIVE),    // blocks r1 only
                booking("r2", "2026-10-20T10:00", "2026-10-20T11:00", BookingStatus.CANCELLED), // ignored
                booking("r3", "2026-10-20T10:00", "2026-10-20T11:00", BookingStatus.CANCELLED), // ignored
                booking("r3", "2026-10-20T10:30", "2026-10-20T11:30", BookingStatus.ACTIVE));   // blocks r3

        List<Room> result = AvailabilityService.findAvailableRooms(rooms, bookings,
                sToTime("2026-10-20T10:00"), sToTime("2026-10-20T11:00"), 4);

        assertEquals(1, result.size());
        assertEquals("r2", result.get(0).getId());
    }
}