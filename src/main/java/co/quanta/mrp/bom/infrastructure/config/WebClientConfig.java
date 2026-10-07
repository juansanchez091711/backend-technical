package co.quanta.mrp.bom.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(OperatorApiProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient operatorsWebClient(WebClient.Builder builder, OperatorApiProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient(properties)))
                .build();
    }

    static HttpClient httpClient(OperatorApiProperties properties) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis())
                .responseTimeout(properties.readTimeout())
                .doOnConnected(connection -> connection.addHandlerLast(
                        new ReadTimeoutHandler(properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS)));
    }
}
