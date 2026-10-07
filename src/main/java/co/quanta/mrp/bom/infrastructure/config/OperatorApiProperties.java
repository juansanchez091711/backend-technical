package co.quanta.mrp.bom.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Settings of the external operators API ({@code quanta.operators-api.*}). */
@ConfigurationProperties("quanta.operators-api")
public record OperatorApiProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public OperatorApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("quanta.operators-api.base-url must be configured");
        }
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
    }
}
