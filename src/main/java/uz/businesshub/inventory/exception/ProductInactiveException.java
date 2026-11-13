package uz.businesshub.inventory.exception;

import java.util.UUID;

public class ProductInactiveException extends RuntimeException {
    public ProductInactiveException(UUID id) { super("Product is not active: " + id); }
}