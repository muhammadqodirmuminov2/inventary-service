package uz.businesshub.inventory.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductSummary getProduct(@PathVariable("id") UUID id);

    record ProductSummary(UUID id, String sku, String name, String status) {}
}