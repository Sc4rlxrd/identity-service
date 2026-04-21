package com.scarlxrd.identity_service.exception;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
