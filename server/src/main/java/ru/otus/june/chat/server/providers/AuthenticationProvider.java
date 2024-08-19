package ru.otus.june.chat.server.providers;

import ru.otus.june.chat.server.ClientHandler;

public interface AuthenticationProvider {

    void initialize();

    void authenticate(ClientHandler clientHandler, String login, String password) throws AuthException;

    void registration(ClientHandler clientHandler, String login, String password, String username, String email, String phoneNumber) throws AuthException;

    boolean authorization(ClientHandler clientHandler, String command);
}
