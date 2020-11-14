package com.krupalshah.composer.exception;

public class ComposerException extends RuntimeException {

    public ComposerException(String message) {
        super(message);
    }

    public ComposerException(Throwable cause) {
        super(cause);
    }
}
