package uz.businesshub.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected StockItem() {}

    public static StockItem empty(UUID companyId, UUID productId) {
        StockItem s = new StockItem();
        s.id = UUID.randomUUID();
        s.companyId = companyId;
        s.productId = productId;
        return s;
    }

    public void receive(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.quantity += amount;
    }

    public int available() {
        return quantity - reserved;
    }

    @PrePersist @PreUpdate
    void touch() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public int getReserved() { return reserved; }
    public Instant getUpdatedAt() { return updatedAt; }
}