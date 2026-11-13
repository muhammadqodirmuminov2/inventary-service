package uz.businesshub.inventory.exception;

public class MissingTenantException extends RuntimeException {
    public MissingTenantException() {
        super("Token does not contain a valid company_id");
    }
}