package io.github.diogohmcruz.api;
import io.github.diogohmcruz.api.dto.TickerInfo;
import io.github.diogohmcruz.domain.service.TickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickers")
@RequiredArgsConstructor
@Tag(name = "Tickers", description = "Stock Ticker Management APIs")
public class TickerController {

  private final TickerService tickerService;

  @Operation(
      summary = "Get all active tickers",
      description = "Retrieves a list of all stock tickers that have active orders or recent trades"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "List of active tickers retrieved successfully",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = String.class))
          )
      )
  })
  @GetMapping
  public ResponseEntity<List<String>> getAllTickers() {
    return ResponseEntity.ok(tickerService.getAllActiveTickers());
  }

  @Operation(
      summary = "Get detailed ticker information",
      description = "Retrieves detailed information for all active tickers including latest prices and order counts"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Ticker information retrieved successfully",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = TickerInfo.class))
          )
      )
  })
  @GetMapping("/info")
  public ResponseEntity<List<TickerInfo>> getTickerInfo() {
    // This will be implemented in the next version
    return ResponseEntity.ok(List.of());
  }
}