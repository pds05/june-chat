package ru.otus.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        sendMessage("Укажите ваше имя: ");
        this.username = in.readUTF().toLowerCase();
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                while (true) {
                    String message = in.readUTF();
                    if (message.equals("/exit")) {
                        sendMessage("/exitok");
                        return;
                    }
                    if (message.startsWith("/auth ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 3) {
                            sendMessage("Неверный формат команды /auth");
                            continue;
                        }
                        if (server.getAuthenticationProvider().authenticate(this, elements[1], elements[2])) {
                            break;
                        }
                        continue;
                    }
                    if (message.startsWith("/register ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 4) {
                            sendMessage("Неверный формат команды /register");
                            continue;
                        }
                        if (server.getAuthenticationProvider().registration(this, elements[1], elements[2], elements[3], AuthorizationRole.USER)) {
                            break;
                        }
                        continue;
                    }
                    sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username'");
                }
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/w")) {
                            String[] elenements = message.split(" ");
                            if (elenements[1].toLowerCase().equals("tom")) {
                                ClientHandler client = server.getClientHandler(elenements[1]);
                                if (client != null) {
                                    client.sendMessage(username + ": " + elenements[2]);
                                }
                            }
                        }
                        if (message.startsWith("/kick ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 2) {
                                sendMessage("Неверный формат команды /kick");
                                continue;
                            }
                            if (server.getAuthenticationProvider() instanceof InMemoryAuthenticationProvider) {
                                if (((InMemoryAuthenticationProvider) server.getAuthenticationProvider()).isAuthorizationRole(username, AuthorizationRole.ADMIN)) {
                                    if (server.kickUsername(elements[1])) {
                                        sendMessage("/kickok пользователь " + elements[1] + " отключен от чата");
                                    } else {
                                        sendMessage("Пользователь " + elements[1] + " отсутствует в чате");
                                    }
                                } else {
                                    sendMessage("Команда /kick недоступна");
                                }
                            } else {
                                sendMessage("Внутренняяя ошибка. Команда /kick временно недоступна");
                            }
                            continue;
                        }
                        continue;
                    }
                    server.broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
