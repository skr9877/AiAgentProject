package com.example.chatbot.config.watsonx;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Profile("watsonx")
public class WatsonxTokenService {

    private static final Logger log = LoggerFactory.getLogger(WatsonxTokenService.class);

    private final WatsonxTokenProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String currentToken = "";

    public WatsonxTokenService(WatsonxTokenProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        refreshToken();
    }

    @Scheduled(fixedDelayString = "${watsonx.auth.refresh-interval-ms:3600000}")
    public void refreshToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", props.getApiKey());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    props.getUrl(), HttpMethod.POST, request, Map.class);

            Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
            if (token != null) {
                currentToken = token.toString();
                log.info("WatsonX 인증 토큰 갱신 완료");
            } else {
                log.warn("WatsonX 인증 응답에 access_token 없음: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("WatsonX 인증 토큰 갱신 실패: {}", e.getMessage());
        }
    }

    public String getToken() {
        return currentToken;
    }
}
