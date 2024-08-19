package ru.otus.june.chat.server.providers.db;

import ru.otus.june.chat.server.AppModule;

import java.sql.Connection;

public interface AppDatabasePool extends AppModule {

//    void initialize();
//
//    void shutdown();

    Connection getConnection();
}
