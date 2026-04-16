package ke.co.masajr.transport.controller;

import ke.co.masajr.transport.service.TicketBookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mpesa")
public class MpesaCallbackController {

    private static final Logger log = LoggerFactory.getLogger(MpesaCallbackController.class);

    private final TicketBookingService bookingService;

    public MpesaCallbackController(TicketBookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * M-PESA STK Push callback endpoint.
     * Safaricom POSTs the result here after the user completes or cancels payment.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestBody Map<String, Object> payload) {
        try {
            log.info("M-PESA callback received: {}", payload);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) payload.get("Body");
            @SuppressWarnings("unchecked")
            Map<String, Object> stkCallback = (Map<String, Object>) body.get("stkCallback");

            String checkoutRequestId = stkCallback.get("CheckoutRequestID").toString();
            int resultCode = Integer.parseInt(stkCallback.get("ResultCode").toString());
            boolean success = resultCode == 0;

            bookingService.handleMpesaCallback(checkoutRequestId, success);

            return ResponseEntity.ok(Map.of("ResultCode", "0", "ResultDesc", "Accepted"));
        } catch (Exception e) {
            log.error("Error processing M-PESA callback: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("ResultCode", "0", "ResultDesc", "Accepted"));
            // Always return 0 to Safaricom to avoid retries
        }
    }
}
