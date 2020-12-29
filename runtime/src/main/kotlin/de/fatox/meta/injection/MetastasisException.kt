package de.fatox.meta.injection;

public class MetastasisException extends RuntimeException {
    MetastasisException(String message) {
        super(message);
    }

    MetastasisException(String message, Throwable cause) {
        super(message, cause);
    }
}
