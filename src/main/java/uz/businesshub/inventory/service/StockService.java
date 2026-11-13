package uz.businesshub.inventory.service;

import feign.FeignException;
import feign.RetryableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.businesshub.inventory.api.dto.StockResponse;
import uz.businesshub.inventory.client.ProductClient;
import uz.businesshub.inventory.client.ProductClient.ProductSummary;
import uz.businesshub.inventory.domain.StockItem;
import uz.businesshub.inventory.exception.DependencyUnavailableException;
import uz.businesshub.inventory.exception.ProductInactiveException;
import uz.businesshub.inventory.exception.ProductNotFoundException;
import uz.businesshub.inventory.repository.StockItemRepository;

import java.util.UUID;

@Service
public class StockService {

    private final StockItemRepository stockRepository;
    private final ProductClient productClient;
    private final TransactionTemplate tx;

    public StockService(StockItemRepository stockRepository, ProductClient productClient,
                        TransactionTemplate tx) {
        this.stockRepository = stockRepository;
        this.productClient = productClient;
        this.tx = tx;
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
        ProductSummary product;
        try {
            product = productClient.getProduct(productId);
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (RetryableException e) {
            throw new DependencyUnavailableException("product-service", e);
        } catch (FeignException e) {
            if (e.status() >= 500) throw new DependencyUnavailableException("product-service", e);
            throw e;
        }
        if (!"ACTIVE".equals(product.status())) {
            throw new ProductInactiveException(productId);
        }
    }
}