package uz.businesshub.inventory.api;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.businesshub.inventory.api.dto.ReceiveStockRequest;
import uz.businesshub.inventory.api.dto.StockResponse;
import uz.businesshub.inventory.security.CurrentCompanyId;
import uz.businesshub.inventory.service.StockService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/{productId}/receive")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public StockResponse receive(@CurrentCompanyId UUID companyId,
                                 @PathVariable UUID productId,
                                 @Valid @RequestBody ReceiveStockRequest request) {
        return stockService.receive(companyId, productId, request.quantity());
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'CASHIER')")
    public StockResponse get(@CurrentCompanyId UUID companyId, @PathVariable UUID productId) {
        return stockService.get(companyId, productId);
    }
}