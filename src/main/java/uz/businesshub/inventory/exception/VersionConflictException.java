package uz.businesshub.inventory.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(long expected, long actual) {
        super("Version conflict: client has " + expected + ", current is " + actual);
    }
}