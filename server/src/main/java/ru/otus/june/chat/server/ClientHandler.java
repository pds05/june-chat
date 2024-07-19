package ru.otus.june.chat.server;

import ru.otus.june.chat.server.providers.AuthenticationProvider;
import ru.otus.june.chat.server.providers.inmemory.InMemoryAuthenticationProvider;
import ru.otus.june.chat.server.providers.jdbc.UserService;
import ru.otus.june.chat.server.providers.jdbc.UserServiceImpl;
import ru.otus.june.chat.server.providers.jdbc.entity.User;
import ru.otus.june.chat.server.providers.jdbc.entity.UserRole;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private User user;
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.username = user.getUsername();
    }

    private boolean authorizeCommand(String message) {
        return server.getAuthenticationProvider().authorization(this, message.split(" ", 2)[0].substring(1));
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                username = socket.getRemoteSocketAddress().toString();
                System.out.println("Подключился новый клиент " + username);
                sendMessage("Пройдите аутентификацию в чате /auth или зарегистрируйтесь /register");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (authorizeCommand(message)) {
                            if (message.equals("/exit")) {
                                sendMessage("/exitok");
                                return;
                            }
                            if (message.startsWith("/auth ")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 3) {
                                    sendMessage("Неверный формат команды. Формат команды: /auth login password");
                                    continue;
                                }
                                if (server.getAuthenticationProvider().authenticate(this, elements[1], elements[2])) {
                                    break;
                                }
                                continue;
                            }
                            if (message.startsWith("/register ")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 6) {
                                    sendMessage("Неверный формат команды. Формат команды: /register login password username email phoneNumber");
                                    continue;
                                }
                                if (server.getAuthenticationProvider().registration(this, elements[1], elements[2], elements[3], elements[4], elements[5])) {
                                    break;
                                }
                                continue;
                            }
                        }
                        continue;
                    }
                    sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username email phoneNumber'");
                }
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (authorizeCommand(message)) {
                            if (message.equals("/exit")) {
                                sendMessage("/exit-ok");
                                break;
                            }
                            if (message.startsWith("/w")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 3) {
                                    sendMessage("Неверный формат команды. Формат команды: /w username message");
                                    continue;
                                }
                                ClientHandler client = server.getClientHandler(elements[1]);
                                if (client != null) {
                                    client.sendMessage(username + ": " + elements[2]);
                                }
                            }
                            if (message.startsWith("/kick ")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 2) {
                                    sendMessage("Неверный формат команды. Формат команды: /kick username");
                                    continue;
                                }
                                if (server.kickUsername(elements[1])) {
                                    sendMessage("/kick-ok пользователь " + elements[1] + " отключен от чата");
                                } else {
                                    sendMessage("Пользователь " + elements[1] + " отсутствует в чате");
                                }
                            }
                            if (message.startsWith("/addrole")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 3) {
                                    sendMessage("Неверный формат команды. Формат команды: /addrole username userRole) ");
                                    continue;
                                }
                                AuthenticationProvider provider = server.getAuthenticationProvider();
                                if (provider instanceof UserService) {
                                    User user = ((UserService) provider).getUser(elements[1], false);
                                    if (user == null) {
                                        sendMessage("/addrole-nok пользователь " + elements[1] + " не обнаружен");
                                        continue;
                                    }
                                    UserRole userRole = ((UserService) provider).getUserRole(elements[2]);
                                    if (userRole == null) {
                                        sendMessage("/addrole-nok роль " + elements[2] + " не обнаружена");
                                        continue;
                                    }
                                    if (((UserService) provider).addUserRoleToUser(user, userRole)) {
                                        sendMessage("/addrole-ok " + user.getUsername() + "= " + user.getUserRoles() + ")");
                                    } else {
                                        sendMessage("/addrole-nok роль не добавлена");
                                    }
                                } else {
                                    sendMessage("/addrole-nok команда временно не доступна");
                                }
                            }
                            if (message.startsWith("/delrole")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 3) {
                                    sendMessage("Неверный формат команды. Формат команды: /delRole username userRole) ");
                                    continue;
                                }
                                AuthenticationProvider provider = server.getAuthenticationProvider();
                                if (provider instanceof UserService) {
                                    User user = ((UserService) provider).getUser(elements[1], false);
                                    if (user == null) {
                                        sendMessage("/delrole-nok пользователь " + elements[1] + " не обнаружен");
                                        continue;
                                    }
                                    UserRole userRole = ((UserService) provider).getUserRole(elements[2]);
                                    if (userRole == null) {
                                        sendMessage("/delrole-nok роль " + elements[2] + " не обнаружена");
                                        continue;
                                    }
                                    if (((UserService) provider).removeUserRoleFromUser(user, userRole)) {
                                        sendMessage("/delrole-ok " + user.getUsername() + "= " + user.getUserRoles() + ")");
                                    } else {
                                        sendMessage("/delrole-nok роль не удалена");
                                    }
                                } else {
                                    sendMessage("/delrole-nok команда временно не доступна");
                                }
                            }
                            if (message.startsWith("/deluser")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 2) {
                                    sendMessage("Неверный формат команды. Формат команды: /deluser username");
                                    continue;
                                }
                                AuthenticationProvider provider = server.getAuthenticationProvider();
                                if (provider instanceof UserService) {
                                    User user = ((UserService) provider).getUser(elements[1], false);
                                    if (user == null) {
                                        sendMessage("/deluser-nok пользователь " + elements[1] + " не обнаружен");
                                        continue;
                                    }
                                    if (((UserServiceImpl) provider).deactivateUser(user)) {
                                        sendMessage("/deluser-ok " + user.getUsername());
                                    } else {
                                        sendMessage("/deluser-nok неуспешное отключение " + user.getUsername());
                                    }
                                } else {
                                    sendMessage("/deluser-nok команда временно не доступна");
                                }
                            }
                            if (message.startsWith("/activate")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 2) {
                                    sendMessage("Неверный формат команды. Формат команды: /activate username");
                                    continue;
                                }
                                AuthenticationProvider provider = server.getAuthenticationProvider();
                                if (provider instanceof UserService) {
                                    User user = ((UserService) provider).getUser(elements[1], false);
                                    if (user == null) {
                                        sendMessage("/activate-nok пользователь " + elements[1] + " не обнаружен");
                                        continue;
                                    }
                                    if (((UserServiceImpl) provider).activateUser(user)) {
                                        sendMessage("/activate-ok " + user.getUsername());
                                    } else {
                                        sendMessage("/activate-nok неуспешное отключение " + user.getUsername());
                                    }
                                } else {
                                    sendMessage("/activate-nok команда временно не доступна");
                                }
                            }
                        }
                        continue;
                    }
                    server.broadcastMessage(username + ": " + message);
                }
            } catch (Exception e) {
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
