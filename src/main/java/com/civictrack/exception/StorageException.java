package com.civictrack.exception;

/** Evidence storage (S3) is not configured, unreachable or misconfigured. */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }
}