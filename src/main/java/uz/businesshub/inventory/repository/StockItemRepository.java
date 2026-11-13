package uz.businesshub.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.businesshub.inventory.domain.StockItem;

import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {
    Optional<StockItem> findByCompanyIdAndProductId(UUID companyId, UUID productId);
}