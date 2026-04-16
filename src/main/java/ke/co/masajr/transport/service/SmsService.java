package ke.co.masajr.transport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String AT_SMS_URL = "https://api.africastalking.com/version1/messaging";

    private final String username;
    private final String apiKey;
    private final WebClient webClient;

    public SmsService(
            @Value("${africastalking.username}") String username,
            @Value("${africastalking.api-key}") String apiKey,
            WebClient.Builder webClientBuilder) {
        this.username = username;
        this.apiKey = apiKey;
        this.webClient = webClientBuilder.baseUrl(AT_SMS_URL).build();
    }

    public void sendSms(String phoneNumber, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Africa's Talking API key not configured – skipping SMS to {}", phoneNumber);
            return;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("username", username);
            form.add("to", phoneNumber);
            form.add("message", message);

            String response = webClient.post()
                    .header("apiKey", apiKey)
                    .header("Accept", "application/json")
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS sent to {} | Response: {}", phoneNumber, response);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
