package uz.businesshub.inventory.exception;

public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String service, Throwable cause) {
        super(service + " is unavailable", cause);
    }
}