package io.github.diogohmcruz.api;

import io.github.diogohmcruz.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.api.dto.OrderResponse;
import io.github.diogohmcruz.domain.model.Order;
import io.github.diogohmcruz.domain.service.OrderBookService;
import io.github.diogohmcruz.domain.service.OrderMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Stock Exchange Order Management APIs")
public class OrderController {
  private final OrderMatchingService orderMatchingService;
  private final OrderBookService orderBookService;

  @Operation(
      summary = "Submit a new order",
      description = "Creates a new buy or sell order in the stock exchange system"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "202",
          description = "Order accepted for processing",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = OrderResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid order data provided"
      )
  })
  @PostMapping
  public ResponseEntity<OrderResponse> submitOrder(
      @Valid @RequestBody CreateOrderRequest request,
      @Valid String userId) {

    Order order = new Order(
        request.getType(),
        request.getTicker(),
        request.getPrice(),
        request.getQuantity(),
        userId
    );

    if (request.getTtlSeconds() != 3600) {
      order.setExpirationTime(order.getTimestamp().plusSeconds(request.getTtlSeconds()));
    }

    orderMatchingService.submitOrder(order);
    return ResponseEntity.accepted().body(OrderResponse.fromOrder(order));
  }

  @Operation(summary = "Get order by ID")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Order found",
          content = @Content(schema = @Schema(implementation = OrderResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Order not found"
      )
  })
  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
    return orderBookService.getOrderById(orderId)
        .map(OrderResponse::fromOrder)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Get all active orders for a ticker")
  @GetMapping("/by-ticker/{ticker}")
  public ResponseEntity<Map<String, List<OrderResponse>>> getOrdersByTicker(
      @Parameter(description = "Stock ticker symbol")
      @PathVariable String ticker) {
    Map<String, List<OrderResponse>> orderBook = Map.of(
        "buyOrders", orderBookService.getActiveBuyOrders(ticker).stream()
            .map(OrderResponse::fromOrder)
            .toList(),
        "sellOrders", orderBookService.getActiveSellOrders(ticker).stream()
            .map(OrderResponse::fromOrder)
            .toList()
    );
    return ResponseEntity.ok(orderBook);
  }

  @Operation(summary = "Get user's orders")
  @GetMapping("/my-orders")
  public ResponseEntity<Page<OrderResponse>> getMyOrders(
      String userId,
      @Parameter(description = "Include inactive orders")
      @RequestParam(defaultValue = "false") boolean includeInactive,
      Pageable pageable) {
    Page<Order> orders = orderBookService.getUserOrders(
        userId,
        includeInactive,
        pageable
    );
    return ResponseEntity.ok(orders.map(OrderResponse::fromOrder));
  }

  @Operation(summary = "Cancel an order")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Order cancelled successfully"),
      @ApiResponse(responseCode = "404", description = "Order not found"),
      @ApiResponse(responseCode = "403", description = "Not authorized to cancel this order")
  })
  @DeleteMapping("/{orderId}")
  public ResponseEntity<Void> cancelOrder(
      @PathVariable UUID orderId,
      String userId) {
    return orderBookService.cancelOrder(orderId, userId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  @Operation(summary = "Get order book statistics")
  @GetMapping("/statistics")
  public ResponseEntity<Map<String, Object>> getOrderBookStats(
      @Parameter(description = "Start time")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant since) {
    return ResponseEntity.ok(orderBookService.getOrderBookStatistics(since));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}
