package ke.co.masajr.transport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripManifestResponse(
    Long tripId,
    String vehicleRegistration,
    int totalSeats,
    int bookedSeats,
    BigDecimal pricePerSeat,
    BigDecimal totalAmount,
    String fromStageName,
    String toDestination,
    LocalDateTime tripStartTime,
    List<PassengerManifestItem> passengers
) {
    public record PassengerManifestItem(
        String ticketId,
        String phoneNumber,
        int passengerCount,
        BigDecimal amount,
        String status,
        String paymentMethod
    ) {}
}
