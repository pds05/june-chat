package ru.otus.june.chat.server.providers.inmemory;

import ru.otus.june.chat.server.providers.AuthenticationProvider;
import ru.otus.june.chat.server.ClientHandler;
import ru.otus.june.chat.server.Server;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuthenticationProvider implements AuthenticationProvider {
    private class User {
        private String login;
        private String password;
        private String username;
        private String email;
        private String phoneNumber;
        private List<AuthorizationRole> roles = new ArrayList<>();

        public User(String login, String password, String username, String email, String phoneNumber, AuthorizationRole role) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.email = email;
            this.phoneNumber = phoneNumber;
            roles.add(role);
        }

        public void addRole(AuthorizationRole role) {
            roles.add(role);
        }

        public void delRole(AuthorizationRole role) {
            roles.remove(role);
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    private Server server;
    private List<User> users;

    public InMemoryAuthenticationProvider(Server server) {
        this.server = server;
        this.users = new ArrayList<>();
        this.users.add(new User("login1", "pass1", "user1", "user1@mail.ru", "79373332211", AuthorizationRole.USER));
        this.users.add(new User("login2", "pass2", "user2", "user2@mail.ru", "79373332212", AuthorizationRole.USER));
        this.users.add(new User("login3", "pass3", "user3", "user3@mail.ru", "79373332213", AuthorizationRole.USER));
        this.users.add(new User("superuser", "superuser", "superuser", "superuser@mail.ru", "79373332210", AuthorizationRole.ADMIN));
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: In-Memory режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthorizationRole(String username, AuthorizationRole role) {
        if (role == AuthorizationRole.USER) {
            return true;
        }
        for (User user : users) {
            if (user.username.equals(username)) {
                for (AuthorizationRole authRole : user.roles) {
                    if (authRole == role || authRole.getPriority() <= role.getPriority()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        if (authUsername == null) {
            clientHandler.sendMessage("Некорретный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username, String email, String phoneNumber) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }
        users.add(new User(login, password, username, email, phoneNumber, AuthorizationRole.USER));
        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/reg-ok " + username);
        return true;
    }

    @Override
    public boolean authorization(ClientHandler clientHandler, String command) {
        AuthorizationRole ownerRole = AuthorizationRole.getAuthorizationRole(command);
        if (ownerRole == null) {
            clientHandler.sendMessage("/" + command + "-nok: комманда не доступна");
            return false;
        }
        if (isAuthorizationRole(clientHandler.getUsername(), ownerRole)) {
            return true;
        } else {
            clientHandler.sendMessage("/" + command + "-nok: комманда не доступна");
            return false;
        }
    }

    public boolean removeUser(User user) {
        return users.remove(user);
    }
}