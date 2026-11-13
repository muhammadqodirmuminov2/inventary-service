package uz.businesshub.inventory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReceiveStockRequest(@Min(1) @Max(1_000_000) int quantity) {}