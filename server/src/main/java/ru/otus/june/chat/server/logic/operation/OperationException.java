package ru.otus.june.chat.server.logic.operation;

import ru.otus.june.chat.server.AppException;

public class OperationException extends AppException {

    public OperationException(String messsage) {
        super(messsage);
    }
}
