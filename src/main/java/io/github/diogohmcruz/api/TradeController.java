package io.github.diogohmcruz.api;

import io.github.diogohmcruz.domain.model.Trade;
import io.github.diogohmcruz.domain.service.TradeService;
import io.github.diogohmcruz.domain.service.TradeService.TradeStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "Trades", description = "Trade Management APIs")
public class TradeController {

  private final TradeService tradeService;

  @Operation(summary = "Get trade by ID", description = "Retrieves a specific trade by its UUID")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Trade found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Trade.class))),
    @ApiResponse(responseCode = "404", description = "Trade not found")
  })
  @GetMapping("/{tradeId}")
  public ResponseEntity<Trade> getTradeById(
      @Parameter(description = "Trade ID") @PathVariable UUID tradeId) {
    return tradeService
        .getTradeById(tradeId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get trades by ticker",
      description = "Retrieves all trades for a specific stock ticker")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "List of trades found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Trade.class)))
  })
  @GetMapping("/by-ticker/{ticker}")
  public ResponseEntity<List<Trade>> getTradesByTicker(
      @Parameter(description = "Stock ticker symbol") @PathVariable String ticker) {
    return ResponseEntity.ok(tradeService.getTradesByTicker(ticker));
  }

  @Operation(
      summary = "Get trades by user",
      description = "Retrieves all trades where the specified user was either buyer or seller")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "List of trades found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Trade.class)))
  })
  @GetMapping("/by-user/{userId}")
  public ResponseEntity<Page<Trade>> getTradesByUser(
      @Parameter(description = "User ID") @PathVariable String userId,
            Pageable pageable) {
    return ResponseEntity.ok(tradeService.getTradesByUser(userId, pageable));
  }

    @Operation(summary = "Get latest prices for all tickers")
    @GetMapping("/latest-prices")
    public ResponseEntity<Map<String, BigDecimal>> getLatestPrices() {
        return ResponseEntity.ok(tradeService.getLatestPrices());
    }

    @Operation(summary = "Get trading statistics")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, TradeStatistics>> getStatistics(
            @Parameter(description = "Start time")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant since) {
        return ResponseEntity.ok(tradeService.getTickerStatistics(since));
    }

    @Operation(summary = "Get daily trading volume")
    @GetMapping("/daily-volume")
    public ResponseEntity<Map<String, Integer>> getDailyVolume() {
        return ResponseEntity.ok(tradeService.getDailyTradingVolume());
    }

    @Operation(summary = "Get user's recent trades")
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Trade>> getUserRecentTrades(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Maximum number of trades to return")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(tradeService.getUserRecentTrades(userId, limit));
    }
}
