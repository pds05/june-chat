INSERT INTO actions (COMMAND, DESCRIPTION)
VALUES ('w', 'персональная отправка сообщения'),
       ('kick', 'отключение пользователя из чата'),
       ('auth', 'аутентификация пользователя'),
       ('register', 'создание нового пользователя'),
       ('deluser', 'удаление пользователя-неактивный статус'),
       ('addrole', 'добавление роли пользователю'),
       ('delrole', 'удаление роли пользователя'),
       ('exit', 'выход из чата'),
       ('activate', 'активирование пользователя');
INSERT INTO user_roles (AUTH_ROLE, DESCRIPTION, PRIORITY)
VALUES ('admin', 'полный доступ', 0),
       ('manager', 'расширенный доступ', 1),
       ('user', 'стандартный доступ', 2);
INSERT INTO user_roles_actions_rel (USER_ROLE_ID, ACTION_ID)
VALUES (1, 6),
       (1, 7),
       (2, 2),
       (2, 5),
       (2, 9),
       (3, 1),
       (3, 3),
       (3, 4),
       (3, 8);
INSERT INTO users_user_roles_rel (USER_ID, USER_ROLE_ID)
VALUES (1, 1),
       (2, 2);