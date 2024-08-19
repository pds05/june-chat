package ru.otus.june.chat.server;

public class AppException extends RuntimeException {

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppException(String message) {
        super(message);
    }
}
