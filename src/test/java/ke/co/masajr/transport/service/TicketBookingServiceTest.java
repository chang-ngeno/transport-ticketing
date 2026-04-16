package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.BookingEntity;
import ke.co.masajr.transport.entity.Fare;
import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.repository.BookingRepository;
import ke.co.masajr.transport.repository.FareRepository;
import ke.co.masajr.transport.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TicketBookingService.
 *
 * Covers all §5 (Quality Assurance) cases from the Engineering Guide:
 *   - Zero args (varargs edge case)
 *   - Mixed nulls in varargs (§2 null-safety)
 *   - Total failure propagation via StructuredTaskScope (§4)
 *   - Virtual thread + Semaphore batch (§3)
 *   - Dynamic pricing resolution
 *   - M-PESA callback handling
 */
@ExtendWith(MockitoExtension.class)
class TicketBookingServiceTest {

    @Mock TripRepository     tripRepository;
    @Mock FareRepository     fareRepository;
    @Mock BookingRepository  bookingRepository;
    @Mock MpesaService       mpesaService;
    @Mock SmsService         smsService;

    @InjectMocks
    TicketBookingService service;

    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        mockTrip = new Trip();
        mockTrip.setId(1L);
        mockTrip.setTenantId(10L);
        mockTrip.setToDestination("Nairobi CBD");
        mockTrip.setTotalSeats(14);
        mockTrip.setBookedSeats(0);
        mockTrip.setPricePerSeat(new BigDecimal("150.00"));
        mockTrip.setDepartureTime(LocalDateTime.now().plusHours(2));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §5 – Varargs edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("§2 & §5 – Varargs null-safety")
    class VarargsNullSafety {

        @Test
        @DisplayName("Zero args: processBatchBookings does not throw")
        void zeroArgs_doesNotThrow() {
            assertDoesNotThrow(() -> service.processBatchBookings(10L, 1L));
        }

        @Test
        @DisplayName("Null array: processBatchBookings returns empty list")
        void nullArray_returnsEmpty() throws Exception {
            List<BookingEntity> result = service.processBatchBookings(10L, 1L, (String[]) null);
            assertThat(result).isEmpty();
            verifyNoInteractions(tripRepository, bookingRepository, mpesaService);
        }

        @Test
        @DisplayName("Mixed nulls: null elements are skipped, valid phones processed")
        void mixedNulls_skipsNullElements() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));
            when(fareRepository.findActiveFare(anyLong(), any())).thenReturn(Optional.empty());
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mpesaService.initiateStk(anyLong(), anyString(), anyDouble(), anyString()))
                    .thenReturn("CHECKOUT-001");

            assertDoesNotThrow(() ->
                service.processBatchBookings(10L, 1L, "+254711000001", null, "+254711000002")
            );

            // Only 2 valid phones should result in 2 booking saves (plus 2 seat updates)
            verify(bookingRepository, atLeast(2)).save(any(BookingEntity.class));
        }

        @Test
        @DisplayName("Zero args: processStrictBatch does not throw")
        void zeroArgs_strictBatch_doesNotThrow() {
            assertDoesNotThrow(() -> service.processStrictBatch(10L, 1L));
        }

        @Test
        @DisplayName("Null array: processStrictBatch returns cleanly")
        void nullArray_strictBatch_returnsCleanly() {
            assertDoesNotThrow(() -> service.processStrictBatch(10L, 1L, (String[]) null));
            verifyNoInteractions(tripRepository);
        }

        @Test
        @DisplayName("Null array: processWithVirtualThreads returns cleanly")
        void nullArray_virtualThreads_returnsCleanly() {
            assertDoesNotThrow(() -> service.processWithVirtualThreads(10L, 1L, (String[]) null));
            verifyNoInteractions(tripRepository);
        }

        @Test
        @DisplayName("Mixed nulls: processWithVirtualThreads skips null elements")
        void mixedNulls_virtualThreads_skipsNulls() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));
            when(fareRepository.findActiveFare(anyLong(), any())).thenReturn(Optional.empty());
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mpesaService.initiateStk(anyLong(), anyString(), anyDouble(), anyString()))
                    .thenReturn("CHECKOUT-VT");

            assertDoesNotThrow(() ->
                service.processWithVirtualThreads(10L, 1L, null, "+254711000003", null)
            );

            verify(bookingRepository, atLeast(1)).save(any(BookingEntity.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §4 – Total failure propagation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("§4 – StructuredTaskScope failure propagation")
    class FailurePropagation {

        @Test
        @DisplayName("processStrictBatch propagates exception from failed sub-task")
        void strictBatch_propagatesFailure() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));
            when(fareRepository.findActiveFare(anyLong(), any())).thenReturn(Optional.empty());
            // Simulate trip not belonging to tenant → runtime exception
            mockTrip.setTenantId(99L); // wrong tenant

            assertThrows(Exception.class, () ->
                service.processStrictBatch(10L, 1L, "+254711000099")
            );
        }

        @Test
        @DisplayName("processBatchBookings throws and returns nothing on sub-task failure")
        void batchBookings_throwsOnFailure() {
            when(tripRepository.findById(1L)).thenReturn(Optional.empty()); // trip not found

            assertThrows(Exception.class, () ->
                service.processBatchBookings(10L, 1L, "+254711000099")
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dynamic pricing
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Dynamic pricing – resolvePrice")
    class DynamicPricing {

        @Test
        @DisplayName("Returns fare window price when active fare exists")
        void returnsActiveFarePrice() {
            Fare fare = new Fare();
            fare.setPricePerSeat(new BigDecimal("200.00"));
            when(fareRepository.findActiveFare(eq(1L), any())).thenReturn(Optional.of(fare));

            BigDecimal price = service.resolvePrice(1L, LocalDateTime.now());

            assertThat(price).isEqualByComparingTo("200.00");
            verify(tripRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Falls back to trip base price when no fare window matches")
        void fallsBackToBasePrice() {
            when(fareRepository.findActiveFare(anyLong(), any())).thenReturn(Optional.empty());
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));

            BigDecimal price = service.resolvePrice(1L, LocalDateTime.now());

            assertThat(price).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("Throws RuntimeException when trip not found and no fare")
        void throwsWhenTripNotFound() {
            when(fareRepository.findActiveFare(anyLong(), any())).thenReturn(Optional.empty());
            when(tripRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () ->
                service.resolvePrice(999L, LocalDateTime.now())
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Single booking
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Single ticket booking")
    class SingleBooking {

        @Test
        @DisplayName("Creates PENDING booking and returns it")
        void createsPendingBooking() throws Exception {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));
            when(fareRepository.findActiveFare(anyLong(), any())).thenReturn(Optional.empty());
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mpesaService.initiateStk(anyLong(), anyString(), anyDouble(), anyString()))
                    .thenReturn("CHECKOUT-123");

            BookingEntity booking = service.bookTicket(10L, 1L, "+254711000001");

            assertThat(booking.getStatus()).isEqualTo("PENDING");
            assertThat(booking.getTicketId()).startsWith("TKT-");
            assertThat(booking.getPricePaid()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("Throws when no seats available")
        void throwsWhenNoSeats() {
            mockTrip.setBookedSeats(14); // fully booked
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));

            assertThrows(RuntimeException.class, () ->
                service.bookTicket(10L, 1L, "+254711000001")
            );
        }

        @Test
        @DisplayName("Throws when trip belongs to different tenant")
        void throwsOnTenantMismatch() {
            mockTrip.setTenantId(99L);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));

            assertThrows(RuntimeException.class, () ->
                service.bookTicket(10L, 1L, "+254711000001")
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // M-PESA callback
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("M-PESA callback handling")
    class MpesaCallback {

        @Test
        @DisplayName("Sets status to PAID on successful callback")
        void setsStatusPaid() {
            BookingEntity booking = new BookingEntity();
            booking.setTicketId("TKT-ABCD1234");
            booking.setPhoneNumber("+254711000001");
            booking.setTripId(1L);
            booking.setStatus("PENDING");

            when(bookingRepository.findByCheckoutRequestId("CHK-001"))
                    .thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.handleMpesaCallback("CHK-001", true);

            assertThat(booking.getStatus()).isEqualTo("PAID");
            verify(smsService).sendSms(eq("+254711000001"), contains("Payment received"));
        }

        @Test
        @DisplayName("Sets status to FAILED and releases seat on failed callback")
        void setsStatusFailedAndReleasesSeat() {
            BookingEntity booking = new BookingEntity();
            booking.setTicketId("TKT-FAIL1234");
            booking.setPhoneNumber("+254711000002");
            booking.setTripId(1L);
            booking.setStatus("PENDING");

            mockTrip.setBookedSeats(3);

            when(bookingRepository.findByCheckoutRequestId("CHK-FAIL"))
                    .thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(tripRepository.findById(1L)).thenReturn(Optional.of(mockTrip));
            when(tripRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.handleMpesaCallback("CHK-FAIL", false);

            assertThat(booking.getStatus()).isEqualTo("FAILED");
            assertThat(mockTrip.getBookedSeats()).isEqualTo(2); // seat released
            verify(smsService).sendSms(eq("+254711000002"), contains("Payment failed"));
        }

        @Test
        @DisplayName("Logs warning and does nothing for unknown checkoutRequestId")
        void doesNothingForUnknownCheckout() {
            when(bookingRepository.findByCheckoutRequestId(anyString()))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.handleMpesaCallback("UNKNOWN", true));
            verifyNoInteractions(smsService, tripRepository);
        }
    }
}
