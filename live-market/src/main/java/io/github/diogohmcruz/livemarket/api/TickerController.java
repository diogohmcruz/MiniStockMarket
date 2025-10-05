package io.github.diogohmcruz.livemarket.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.diogohmcruz.livemarket.service.TickerStatsCache;
import io.github.diogohmcruz.marketlibrary.api.dto.TickerInfo;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/tickers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TickerController {

    private final TickerStatsCache cache;

    @GetMapping
    public Flux<TickerInfo> listAll() {
        return Flux.fromIterable(cache.listAll());
    }

    @GetMapping("/{symbol}")
    public Mono<TickerInfo> bySymbol(@PathVariable String symbol) {
        var snapshot = cache.getTicker(symbol);
        if (snapshot == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticker not found"));
        }
        return Mono.just(snapshot);
    }
}
