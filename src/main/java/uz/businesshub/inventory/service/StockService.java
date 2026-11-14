package uz.businesshub.inventory.service;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.businesshub.inventory.api.dto.StockResponse;
import uz.businesshub.inventory.client.ProductClient;
import uz.businesshub.inventory.client.ProductClient.ProductSummary;
import uz.businesshub.inventory.config.ResilienceConfig;
import uz.businesshub.inventory.domain.StockItem;
import uz.businesshub.inventory.exception.DependencyUnavailableException;
import uz.businesshub.inventory.exception.ProductInactiveException;
import uz.businesshub.inventory.exception.ProductNotFoundException;
import uz.businesshub.inventory.repository.StockItemRepository;

import java.util.UUID;
import java.util.function.Supplier;

@Service
public class StockService {

    private final StockItemRepository stockRepository;
    private final ProductClient productClient;
    private final TransactionTemplate tx;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public StockService(StockItemRepository stockRepository, ProductClient productClient,
                        TransactionTemplate tx, CircuitBreaker circuitBreaker,
                        Retry retry, Bulkhead bulkhead) {
        this.stockRepository = stockRepository;
        this.productClient = productClient;
        this.tx = tx;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.bulkhead = bulkhead;
    }

    public StockResponse receive(UUID companyId, UUID productId, int amount) {
        // 1. Tarmoq chaqiruvi: TRANZAKSIYADAN TASHQARIDA
        ensureActiveProduct(productId);

        // 2. Faqat baza ishi: qisqa tranzaksiya
        return tx.execute(status -> {
            StockItem item = stockRepository.findByCompanyIdAndProductId(companyId, productId)
                    .orElseGet(() -> StockItem.empty(companyId, productId));
            item.receive(amount);
            stockRepository.saveAndFlush(item);
            return StockResponse.from(item);
        });
    }

    public StockResponse get(UUID companyId, UUID productId) {
        return stockRepository.findByCompanyIdAndProductId(companyId, productId)
                .map(StockResponse::from)
                .orElseGet(() -> StockResponse.empty(productId));
    }

    private void ensureActiveProduct(UUID productId) {
        Supplier<ProductSummary> call = () -> productClient.getProduct(productId);
        call = Bulkhead.decorateSupplier(bulkhead, call);
        call = CircuitBreaker.decorateSupplier(circuitBreaker, call);
        call = Retry.decorateSupplier(retry, call);

        ProductSummary product;
        try {
            product = call.get();
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (CallNotPermittedException | BulkheadFullException e) {
            throw new DependencyUnavailableException("product-service", e);
        } catch (FeignException e) {
            if (ResilienceConfig.isInfraFailure(e)) throw new DependencyUnavailableException("product-service", e);
            throw e;
        }
        if (!"ACTIVE".equals(product.status())) {
            throw new ProductInactiveException(productId);
        }
    }
}