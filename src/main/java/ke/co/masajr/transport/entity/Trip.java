package ke.co.masajr.transport.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip")
@Data
public class Trip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "from_stage_id", nullable = false)
    private Long fromStageId;

    @Column(name = "to_stage_id")
    private Long toStageId;

    @Column(name = "to_destination", length = 200)
    private String toDestination;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(length = 200)
    private String route;

    @Column(name = "trip_start_time", nullable = false)
    private LocalDateTime tripStartTime;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(name = "booked_seats")
    private Integer bookedSeats = 0;

    @Column(name = "price_per_seat", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @Column(length = 20)
    private String status = "BOARDING"; // BOARDING, TRAVELLING, ENDED
}
