package ru.otus.june.chat.server.logic;

import ru.otus.june.chat.server.ClientHandler;
import ru.otus.june.chat.server.Server;

public interface LogicProcessor {
    void process(Server server, ClientHandler clientHandler, String inputMessage);
}
