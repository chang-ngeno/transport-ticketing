package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Tenant;
import ke.co.masajr.transport.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

@Service
public class MpesaService {

    private static final Logger log = LoggerFactory.getLogger(MpesaService.class);
    private static final String MPESA_BASE_URL = "https://sandbox.safaricom.co.ke";
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;
    private final WebClient webClient;
    private final String callbackUrl;

    public MpesaService(
            TenantRepository tenantRepository,
            EncryptionService encryptionService,
            WebClient.Builder webClientBuilder,
            @Value("${mpesa.callback-url}") String callbackUrl) {
        this.tenantRepository = tenantRepository;
        this.encryptionService = encryptionService;
        this.webClient = webClientBuilder.baseUrl(MPESA_BASE_URL).build();
        this.callbackUrl = callbackUrl;
    }

    private String getAccessToken(String consumerKey, String consumerSecret) {
        String credentials = Base64.getEncoder()
                .encodeToString((consumerKey + ":" + consumerSecret).getBytes());

        Map<?, ?> response = webClient.get()
                .uri("/oauth/v1/generate?grant_type=client_credentials")
                .header("Authorization", "Basic " + credentials)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain M-PESA access token");
        }
        return (String) response.get("access_token");
    }

    public String initiateStk(Long tenantId, String phoneNumber, double amount, String ticketId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        String salt = tenant.getMpesaEncryptionSalt();
        String consumerKey = encryptionService.decrypt(tenant.getMpesaConsumerKeyEncrypted(), salt);
        String consumerSecret = encryptionService.decrypt(tenant.getMpesaConsumerSecretEncrypted(), salt);
        String passkey = encryptionService.decrypt(tenant.getMpesaPasskeyEncrypted(), salt);
        String shortcode = tenant.getMpesaShortcode();

        String accessToken = getAccessToken(consumerKey, consumerSecret);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String password = Base64.getEncoder()
                .encodeToString((shortcode + passkey + timestamp).getBytes());

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", shortcode);
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("TransactionType", "CustomerPayBillOnline");
        payload.put("Amount", (long) amount);
        payload.put("PartyA", phoneNumber);
        payload.put("PartyB", shortcode);
        payload.put("PhoneNumber", phoneNumber);
        payload.put("CallBackURL", callbackUrl);
        payload.put("AccountReference", ticketId);
        payload.put("TransactionDesc", "Ticket " + ticketId);

        Map<?, ?> response = webClient.post()
                .uri("/mpesa/stkpush/v1/processrequest")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("CheckoutRequestID")) {
            throw new RuntimeException("STK push failed for ticket " + ticketId);
        }

        log.info("STK push initiated for ticket {} | CheckoutRequestID: {}", ticketId, response.get("CheckoutRequestID"));
        return (String) response.get("CheckoutRequestID");
    }
}
