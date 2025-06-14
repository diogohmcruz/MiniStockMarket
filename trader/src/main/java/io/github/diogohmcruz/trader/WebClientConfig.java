package io.github.diogohmcruz.trader;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  @Value("${stockexchange.api.base-url:http://localhost:8080}")
  private String orderApiUrl;

  @Bean
  public WebClient webClient() {
    HttpClient httpClient = HttpClient.newHttpClient();
    return WebClient.builder()
        .clientConnector(new JdkClientHttpConnector(httpClient))
        .baseUrl(orderApiUrl)
        .build();
  }
}
