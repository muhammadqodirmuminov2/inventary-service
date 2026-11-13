package uz.businesshub.inventory.api.dto;

import uz.businesshub.inventory.domain.StockItem;
import java.time.Instant;
import java.util.UUID;

public record StockResponse(UUID productId, int quantity, int reserved, int available, Instant updatedAt) {
    public static StockResponse from(StockItem s) {
        return new StockResponse(s.getProductId(), s.getQuantity(), s.getReserved(),
                s.available(), s.getUpdatedAt());
    }
    public static StockResponse empty(UUID productId) {
        return new StockResponse(productId, 0, 0, 0, null);
    }
}