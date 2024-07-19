package ru.otus.june.chat.server.providers;

import ru.otus.june.chat.server.ClientHandler;

public interface AuthenticationProvider {
    void initialize();

    boolean authenticate(ClientHandler clientHandler, String login, String password);

    boolean registration(ClientHandler clientHandler, String login, String password, String username, String email, String phoneNumber);

    boolean authorization(ClientHandler clientHandler, String command);
}
