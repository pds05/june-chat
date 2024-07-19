package ru.otus.june.chat.server;

public class ServerApplication {
    public static void main(String[] args) {
        Server server = new Server(8189);
        server.start();
        System.out.println("Сервер остановлен");
    }
}
