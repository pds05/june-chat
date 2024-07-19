package ru.otus.june.chat.server.providers.jdbc;

import ru.otus.june.chat.server.ClientHandler;
import ru.otus.june.chat.server.Server;
import ru.otus.june.chat.server.providers.jdbc.entity.User;
import ru.otus.june.chat.server.providers.jdbc.entity.UserActivity;
import ru.otus.june.chat.server.providers.jdbc.entity.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserServiceImpl implements UserService {
    public static final String DB_URL = "jdbc:mariadb://localhost:3306/chat_db";

    public static final String AUTH_USER_QUERY = "SELECT * FROM users WHERE login = ? AND password = ?";
    public static final String USERS_QUERY = "SELECT * FROM users";
    public static final String USER_BY_USERNAME_QUERY = "SELECT * FROM users WHERE username = ?";
    public static final String USER_BY_LOGIN_QUERY = "SELECT * FROM users WHERE login = ?";
    public static final String USER_ROLES_OF_USER_QUERY = """
            SELECT ur.* FROM user_roles ur
            JOIN users_user_roles_rel uurr ON ur.ID = uurr.USER_ROLE_ID
            WHERE uurr.USER_ID = ?
            ORDER BY ur.PRIORITY ASC
            """;
    public static final String USER_ROLE_QUERY = "SELECT * FROM user_roles WHERE auth_role = ?";
    public static final String USER_ROLES_QUERY = "SELECT * FROM user_roles ORDER BY PRIORITY DESC";
    public static final String ADD_USER_QUERY = """
            INSERT INTO users (USERNAME, LOGIN, PASSWORD, EMAIL, PHONE_NUMBER, IS_ACTIVE, REGISTRATION_DATE) 
            VALUES (?, ?, ?, ?, ? ,?, ?)
            """;
    public static final String ADD_USER_ROLE_QUERY = "INSERT INTO user_roles (AUTH_ROLE, DESCRIPTION) VALUES (?, ?)";
    public static final String ADD_USER_ROLE_TO_USER_QUERY = "INSERT INTO users_user_roles_rel (USER_ID, USER_ROLE_ID) VALUES (?, ?)";
    public static final String IS_USER_ROLE_OF_USER_QUERY = """
            SELECT COUNT(*) FROM user_roles ur
            JOIN users_user_roles_rel uurr ON ur.ID = uurr.USER_ROLE_ID
            WHERE uurr.USER_ID = ? AND ur.ID = ?
            """;
    public static final String DELETE_USER_QUERY = "DELETE FROM users WHERE ID = ?";
    public static final String DELETE_USER_ROLE_FROM_USER_QUERY = """
            DELETE FROM users_user_roles_rel
            WHERE USER_ID = ? AND USER_ROLE_ID = ?
            """;
    public static final String UPDATE_USER_QUERY = """
            UPDATE users
            SET USERNAME = ?, LOGIN = ?, PASSWORD = ?, EMAIL = ?, PHONE_NUMBER = ?, IS_ACTIVE = ?, DEACTIVATION_DATE = ?
            WHERE ID = ?
            """;
    public static final String USERS_ACTIVITY_QUERY = "SELECT * FROM users_activity WHERE USER_ID = ?";
    public static final String ADD_USERS_ACTIVITY_QUERY = "INSERT INTO users_activity (USER_ID, LAST_CONNECT_DATE, IS_ONLINE) VALUES (?, ?, ?)";
    public static final String UPDATE_USERS_ACTIVITY_QUERY = """
            UPDATE users_activity
            SET LAST_CONNECT_DATE = ?, LAST_DISCONNECT_DATE = ?, KICK_DATE = ?, IS_ONLINE = ?
            WHERE USER_ID = ?
            """;
    public static final String AUTHORIZATION_QUERY = """
            SELECT COUNT(*) FROM user_roles ur,
                (SELECT ur2.PRIORITY FROM actions a2
                JOIN user_roles_actions_rel urar2 ON urar2.ACTION_ID = a2.ID
                JOIN user_roles ur2 ON urar2.USER_ROLE_ID = ur2.ID
                WHERE a2.COMMAND = ?) s2
            WHERE ur.ID = ? and ur.PRIORITY <= s2.PRIORITY;
            """;

    private Server server;
    private Connection connection;

    public UserServiceImpl(Server server) {
        this.server = server;
        initialize();
    }

    private List<User> readUsers(ResultSet resultSet) throws SQLException {
        List<User> users = new ArrayList<>();
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String username = resultSet.getString("username");
            String login = resultSet.getString("login");
            String password = resultSet.getString("password");
            String email = resultSet.getString("email");
            long phoneNumber = resultSet.getLong("phone_number");
            boolean isActive = resultSet.getBoolean("is_active");
            Date registrationDate = resultSet.getTimestamp("registration_date");
            Date deactivationDate = resultSet.getTimestamp("deactivation_date");

            User user = new User(id, username, login, password, email, phoneNumber, isActive, registrationDate, deactivationDate);
            List<UserRole> userRoles = getUserRolesOfUser(user);
            user.setUserRoles(userRoles);

            UserActivity userActivity = getUserActivity(user);
            user.setActivity(userActivity);

            users.add(user);
        }
        return users;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(USERS_QUERY)) {
                users.addAll(readUsers(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (User user : users) {
            List<UserRole> userRoles = getUserRolesOfUser(user);
            user.setUserRoles(userRoles);

            UserActivity userActivity = getUserActivity(user);
            user.setActivity(userActivity);
        }
        return users;
    }

    @Override
    public User getUser(String name, boolean byLogin) {
        User user = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(byLogin ? USER_BY_LOGIN_QUERY : USER_BY_USERNAME_QUERY)) {
            preparedStatement.setString(1, name);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                List<User> users = readUsers(rs);
                if (users.size() == 1) {
                    user = users.get(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

//    private String fillQueryParameters(String query, String ...parameters) {
//        for (String parameter : parameters) {
//            query.replaceFirst("[?]", parameter);
//        }
//        return query;
//    }

    @Override
    public boolean addUser(User user) {
        if (user == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getPassword());
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.setLong(5, user.getPhoneNumber());
            preparedStatement.setBoolean(6, user.isActive());
            preparedStatement.setTimestamp(7, new Timestamp(new Date().getTime()));
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateUser(User user, boolean updateUserRoles) {
        if (user == null || user.getId() == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_QUERY)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getPassword());
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.setLong(5, user.getPhoneNumber());
            preparedStatement.setBoolean(6, user.isActive());
            preparedStatement.setTimestamp(7, user.getDeactivationDate() != null ? new Timestamp(user.getDeactivationDate().getTime()) : null);
            preparedStatement.setInt(8, user.getId());
            int flag = preparedStatement.executeUpdate();
            if (updateUserRoles) {
                List<UserRole> localUserRoles = user.getUserRoles();
                if (localUserRoles != null || localUserRoles.isEmpty()) {
                    List<UserRole> dbUserRoles = getUserRolesOfUser(user);
                    for (UserRole userRole : localUserRoles) {
                        if (!dbUserRoles.contains(userRole)) {
                            addUserRoleToUser(user, userRole);
                        }
                    }
                    for (UserRole userRole : dbUserRoles) {
                        if (!localUserRoles.contains(userRole)) {
                            removeUserRoleFromUser(user, userRole);
                        }
                    }
                }
            }
            return flag == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deactivateUser(User user) {
        if (user == null) {
            return false;
        }
        user.setActive(false);
        return updateUser(user, false);
    }

    public boolean activateUser(User user) {
        if (user == null) {
            return false;
        }
        user.setActive(true);
        return updateUser(user, false);
    }

    @Override
    public boolean deleteUser(User user) {
        if (user == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public UserRole getUserRole(String userRoleName) {
        UserRole userRole = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(USER_ROLE_QUERY)) {
            preparedStatement.setString(1, userRoleName);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String authRole = rs.getString("auth_role");
                    String description = rs.getString("description");
                    int priority = rs.getInt("priority");
                    userRole = new UserRole(id, authRole, description, priority);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userRole;
    }

    @Override
    public List<UserRole> getUserRoles() {
        List<UserRole> userRoles = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(USER_ROLES_QUERY)) {
                userRoles.addAll(readUserRoles(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userRoles;
    }

    @Override
    public List<UserRole> getUserRolesOfUser(User user) {
        List<UserRole> userRoles = new ArrayList<>();
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(USER_ROLES_OF_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                userRoles.addAll(readUserRoles(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userRoles;
    }

    private List<UserRole> readUserRoles(ResultSet resultSet) throws SQLException {
        List<UserRole> userRoles = new ArrayList<>();
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String authRole = resultSet.getString("auth_role");
            String description = resultSet.getString("description");
            int priority = resultSet.getInt("priority");
            UserRole userRole = new UserRole(id, authRole, description, priority);
            userRoles.add(userRole);
        }
        return userRoles;
    }

    @Override
    public boolean addUserRole(UserRole userRole) {
        if (userRole == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_ROLE_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, userRole.getAuthRole());
            preparedStatement.setString(2, userRole.getDescription());
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                userRole.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isUserRoleOfUser(User user, UserRole userRole) {
        if (user == null || userRole == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(IS_USER_ROLE_OF_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    int flag = rs.getInt(1);
                    return flag == 1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public UserActivity getUserActivity(User user) {
        UserActivity userActivity = null;
        if (user == null || user.getId() == null) {
            return null;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(USERS_ACTIVITY_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int userId = rs.getInt("user_id");
                    Date lastConnectDate = rs.getTimestamp("last_connect_date");
                    Date lastDisconnectDate = rs.getTimestamp("last_disconnect_date");
                    Date kickDate = rs.getTimestamp("kick_date");
                    boolean isOnline = rs.getBoolean("is_online");
                    userActivity = new UserActivity(id, userId, lastConnectDate, lastDisconnectDate, kickDate, isOnline);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userActivity;
    }

    @Override
    public boolean addUserActivity(UserActivity userActivity) {
        if (userActivity == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(ADD_USERS_ACTIVITY_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, userActivity.getUserId());
            preparedStatement.setTimestamp(2, new Timestamp(userActivity.getLastConnectDate().getTime()));
            preparedStatement.setBoolean(3, userActivity.isOnline());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                userActivity.setId(resultSet.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateUserActivity(UserActivity userActivity) {
        if (userActivity == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USERS_ACTIVITY_QUERY)) {
            preparedStatement.setTimestamp(1, userActivity.getLastConnectDate() != null ? new Timestamp(userActivity.getLastConnectDate().getTime()) : null);
            preparedStatement.setTimestamp(2, userActivity.getLastDisconnectDate() != null ? new Timestamp(userActivity.getLastDisconnectDate().getTime()) : null);
            preparedStatement.setTimestamp(3, userActivity.getKickDate() != null ? new Timestamp(userActivity.getKickDate().getTime()) : null);
            preparedStatement.setBoolean(4, userActivity.isOnline());
            preparedStatement.setInt(5, userActivity.getUserId());
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean addUserRoleToUser(User user, UserRole userRole) {
        if (user == null || userRole == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_ROLE_TO_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            int flag = preparedStatement.executeUpdate();
            if (flag == 1) {
                user.setUserRoles(getUserRolesOfUser(user));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean removeUserRoleFromUser(User user, UserRole userRole) {
        if (user == null || userRole == null) {
            return false;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_ROLE_FROM_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            int flag = preparedStatement.executeUpdate();
            if (flag == 1) {
                user.setUserRoles(getUserRolesOfUser(user));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void connectUser(User user) {
        if (user.getId() == null) {
            System.out.println("Ошибка обвноления журнала активности: user id is null " + user);
        }
        if (user.getActivity() == null) {
            UserActivity userActivity = new UserActivity(user.getId(), new Date(), null, null, true);
            if (addUserActivity(userActivity)) {
                user.setActivity(userActivity);
                return;
            }
        } else {
            UserActivity userActivity = user.getActivity();
            userActivity.setLastConnectDate(new Date());
            userActivity.setOnline(true);
            if (updateUserActivity(userActivity)) {
                user.setActivity(userActivity);
                return;
            }
        }
        System.out.println("Журнал активности не обвнолен: " + user);
    }

    @Override
    public void disconnectUser(User user, boolean isKicked) {
        if (user.getId() == null) {
            System.out.println("Ошибка обвноления журнала активности: user id is null " + user);
        }
        UserActivity userActivity = user.getActivity();
        if (userActivity == null) {
            System.out.println("Ошибка обвноления журнала активности: userActivity is null " + user);
        }
        userActivity.setLastDisconnectDate(!isKicked ?
                new Timestamp(new Date().getTime()) :
                userActivity.getLastDisconnectDate() != null ?
                        new Timestamp(userActivity.getLastDisconnectDate().getTime()) :
                        null);
        userActivity.setKickDate(isKicked ?
                new Timestamp(new Date().getTime()) :
                userActivity.getKickDate() != null ?
                        new Timestamp(userActivity.getKickDate().getTime()) :
                        null);
        userActivity.setOnline(false);
        if (updateUserActivity(userActivity)) {
            user.setActivity(userActivity);
        } else {
            System.out.println("Журнал активности не обновлен: " + user);
        }
    }

    @Override
    public void initialize() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(DB_URL, "chat_admin", "12345678");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Системная ошибка: не удалось подключиться к базе данных " + DB_URL);
                server.stop();
            }
        }
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(AUTH_USER_QUERY)) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                List<User> users = readUsers(rs);
                if (users.isEmpty()) {
                    clientHandler.sendMessage("/auth-nok: некорретный логин/пароль");
                    return false;
                }
                User user = users.get(0);
                if (!user.isActive()) {
                    clientHandler.sendMessage("/auth-nok: учетная запись отключена");
                    return false;
                }
                if (server.isUsernameBusy(user.getUsername())) {
                    clientHandler.sendMessage("/auth-nok: учетная запись занята");
                    return false;
                }
                clientHandler.setUser(user);
                server.subscribe(clientHandler);
                clientHandler.sendMessage("/auth-ok: " + user.getUsername() + " подключен к чату");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Системная ошибка аутентификации клиента: " + clientHandler.getUsername());
            clientHandler.sendMessage("/auth-nok: внутренняя ошибка, сервер не доступен");
        }
        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username, String email, String phoneNumber) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
            return false;
        }
        User user = new User(username, login, password, email, Long.valueOf(phoneNumber), true, new Date());
        addUser(user);
        if (user.getId() != null) {
            clientHandler.setUser(user);
            server.subscribe(clientHandler);
            clientHandler.sendMessage("/register-ok: создана учетная запись " + user.getUsername());
            return true;
        }
        if (getUser(username, false) != null) {
            clientHandler.sendMessage("/register-nok: имя пользователя занято");
        }
        if (getUser(login, true) != null) {
            clientHandler.sendMessage("/register-nok: логин занят");
        }
        return false;
    }

    @Override
    public boolean authorization(ClientHandler clientHandler, String command) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(AUTHORIZATION_QUERY)) {
            preparedStatement.setString(1, command);
            User user = clientHandler.getUser();
            if (user != null) {
                List<UserRole> userRoles = user.getUserRoles();
                //если у пользователя есть группы, то извлекаем с наивысшем приоритетом, иначе используем группу с наименьшим доступом
                preparedStatement.setInt(2, user.getUserRoles() != null ? userRoles.get(0).getId() : getUserRoles().get(0).getId());
            } else {
                preparedStatement.setInt(2, getUserRole("user").getId());
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int flag = resultSet.getInt(1);
                    if (flag >= 1) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        clientHandler.sendMessage("/" + command + "-nok: комманда не доступна");
        return false;
    }
}
