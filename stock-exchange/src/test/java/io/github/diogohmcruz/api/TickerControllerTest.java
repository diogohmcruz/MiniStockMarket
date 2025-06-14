package io.github.diogohmcruz.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.diogohmcruz.stockexchange.api.TickerController;
import io.github.diogohmcruz.stockexchange.domain.service.TickerService;

@WebMvcTest(TickerController.class)
class TickerControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TickerService tickerService;

  @Test
  void getAllTickers_ShouldReturnListOfTickers() throws Exception {
    // Given
    List<String> tickers = List.of("AAPL", "GOOGL", "MSFT");
    when(tickerService.getAllActiveTickers()).thenReturn(tickers);

    // When & Then
    mockMvc
        .perform(get("/api/tickers"))
        .andExpect(status().isOk())
        .andExpect(content().json("[\"AAPL\",\"GOOGL\",\"MSFT\"]"));
  }
}
