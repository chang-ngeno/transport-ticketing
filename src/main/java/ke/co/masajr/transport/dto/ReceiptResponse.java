package ke.co.masajr.transport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptResponse(
    String ticketId,
    String phoneNumber,
    String paymentMethod,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt,
    int passengerCount,
    Long tripId,
    String fromStageName,
    String toStageName,
    String toDestination,
    String route,
    LocalDateTime tripStartTime,
    String vehicleRegistration,
    String tenantName
) {}
