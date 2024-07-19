package ru.otus.june.chat.server;

import ru.otus.june.chat.server.providers.AuthenticationProvider;
import ru.otus.june.chat.server.providers.jdbc.UserService;
import ru.otus.june.chat.server.providers.jdbc.UserServiceImpl;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private int port;
    private Map<String, ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private boolean isStopped;

    public Server(int port) {
        this.port = port;
        this.clients = new HashMap<>();
        this.authenticationProvider = new UserServiceImpl(this);
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }


    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            authenticationProvider.initialize();
            while (!isStopped) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        System.out.println("Завершается работа сервера...");
        clients.forEach((k, v) -> kickUsername(k));
        isStopped = true;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        if (authenticationProvider instanceof UserService) {
            ((UserService) authenticationProvider).connectUser(clientHandler.getUser());
        }
        broadcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.put(clientHandler.getUsername(), clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        if (authenticationProvider instanceof UserService) {
            ((UserService) authenticationProvider).disconnectUser(clientHandler.getUser(), false);
        }
        clients.remove(clientHandler.getUsername());
        broadcastMessage("Из чата вышел: " + clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            entry.getValue().sendMessage(message);
        }
    }

    public ClientHandler getClientHandler(String username) {
        return clients.get(username);
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler c : clients.values()) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean kickUsername(String username) {
        for (ClientHandler c : clients.values()) {
            if (c.getUsername().equals(username)) {
                c.sendMessage("Вы отключены от чата");
                if (authenticationProvider instanceof UserService) {
                    ((UserService) authenticationProvider).disconnectUser(c.getUser(), true);
                }
                c.disconnect();
                return true;
            }
        }
        return false;
    }
}
