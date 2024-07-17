package ru.otus.june.chat.server;

import ru.otus.june.chat.server.entity.User;
import ru.otus.june.chat.server.entity.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserServiceImpl implements UserService {
    public static final String DB_URL = "jdbc:mariadb://localhost:3306/chat_db";

    public static final String AUTH_USER_QUERY = "SELECT * FROM user WHERE login = ? AND password = ?";
    public static final String USERS_QUERY = "SELECT * FROM user";
    public static final String USER_QUERY = "SELECT * FROM user WHERE username = ?";
    public static final String USER_ROLES_OF_USER_QUERY = """
            SELECT ur.* FROM user_roles ur
            JOIN user_roles_rel urr ON ur.ID = urr.USER_ROLE_ID
            WHERE urr.USER_ID = ?
            """;
    public static final String USER_ROLES_QUERY = "SELECT * FROM user_roles";
    public static final String ADD_USER_QUERY = """
            INSERT INTO users (USERNAME, LOGIN, PASSWORD, EMAIL, PHONE_NUMBER, IS_ACTIVE, REGISTRATION_DATE) 
            VALUES (?, ?, ?, ?, ? ,?, ?)
            """;
    public static final String ADD_USER_ROLE_QUERY = "INSERT INTO user_roles (AUTH_ROLE, DESCRIPTION) VALUES (?, ?)";
    public static final String ADD_USER_ROLE_TO_USER_QUERY = "INSERT INTO user_roles_rel (USER_ID, USER_ROLE_ID) VALUES (?, ?)";
    public static final String IS_USER_ROLE_OF_USER_QUERY = """
            SELECT COUNT(*) FROM user_roles ur
            JOIN user_roles_rel urr ON ur.ID = urr.USER_ROLE_ID
            WHERE urr.USER_ID = ? AND ur.ID = ?
            """;
    public static final String DELETE_USER_QUERY = "DELETE FROM users WHERE ID = ?";
    public static final String DELETE_USER_ROLE_FROM_USER_QUERY = """
            DELETE FROM user_roles_rel
            WHERE USER_ID = ? AND USER_ROLE_ID = ?
            """;
    public static final String UPDATE_USER_QUERY = """
            UPDATE users
            SET USERNAME=?, LOGIN=?, PASSWORD=?, EMAIL=?, PHONE_NUMBER=?, IS_ACTIVE=?, KICK_DATE=?
            WHERE ID = ?
            """;
    public static final String ADD_ONLINE_USER_QUERY = "INSERT INTO online_users (USER_ID, CONNECTION_TIME) VALUES (?, ?)";
    public static final String DELETE_ONLINE_USER_QUERY = "DELETE FROM online_users WHERE USER_ID = ?";

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
            Date registrationDate = resultSet.getDate("registration_date");
            Date kickDate = resultSet.getDate("kick_date");
            User user = new User(id, username, login, password, email, phoneNumber, isActive, registrationDate, kickDate);
            users.add(user);
        }
        return users;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try(Statement stmt = connection.createStatement()) {
            try(ResultSet rs = stmt.executeQuery(USERS_QUERY)) {
                users.addAll(readUsers(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for(User user : users) {
            List<UserRole> userRoles = getUserRolesOfUser(user);
            user.setUserRoles(userRoles);
        }
        return users;
    }

    @Override
    public User getUser(String username) {
        User user = null;
        try(PreparedStatement preparedStatement = connection.prepareStatement(USER_QUERY)){
            preparedStatement.setString(1, username);
            try(ResultSet rs = preparedStatement.executeQuery()) {
                List<User> users = readUsers(rs);
                if(users.size() == 1) {
                    user = users.get(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public User addUser(User user) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)){
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getPassword());
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.setLong(5, user.getPhoneNumber());
            preparedStatement.setBoolean(6, user.isActive());
            preparedStatement.setTimestamp(7, new Timestamp(new Date().getTime()));
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if(rs.next()) {
                user.setId(rs.getInt(1));
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public boolean updateUser(User user) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_QUERY)){
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getPassword());
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.setLong(5, user.getPhoneNumber());
            preparedStatement.setBoolean(6, user.isActive());
            preparedStatement.setTimestamp(7, user.getKickDate() != null ? new Timestamp(user.getKickDate().getTime()) : null);
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteUser(User user) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_QUERY)){
            preparedStatement.setInt(1, user.getId());
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<UserRole> getUserRoles() {
        List<UserRole> userRoles = new ArrayList<>();
        try(Statement statement = connection.createStatement()){
            try(ResultSet rs = statement.executeQuery(USER_ROLES_QUERY)) {
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String authRole = rs.getString("auth_role");
                    String description = rs.getString("description");
                    UserRole userRole = new UserRole(id, authRole, description);
                    userRoles.add(userRole);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userRoles;
    }

    @Override
    public List<UserRole> getUserRolesOfUser(User user) {
        List<UserRole> userRoles = new ArrayList<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(USER_ROLES_OF_USER_QUERY)){
            preparedStatement.setInt(1, user.getId());
            try(ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String authRole = rs.getString("auth_role");
                    String description = rs.getString("description");
                    UserRole userRole = new UserRole(id, authRole, description);
                    userRoles.add(userRole);

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userRoles;
    }

    @Override
    public UserRole addUserRole(UserRole userRole) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_ROLE_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)){
            preparedStatement.setString(1, userRole.getAuthRole());
            preparedStatement.setString(2, userRole.getDescription());
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if(rs.next()) {
                userRole.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userRole;
    }

    @Override
    public boolean isUsersUserRole(User user, UserRole userRole) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(IS_USER_ROLE_OF_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            try(ResultSet rs = preparedStatement.executeQuery()) {
                if(rs.next()) {
                    int flag = rs.getInt(1);
                    if(flag == 1) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public User addUserRoleToUser(User user, UserRole userRole) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_ROLE_TO_USER_QUERY)){
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            preparedStatement.executeUpdate();
            user.setUserRoles(getUserRolesOfUser(user));
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public User removeUserRoleFromUser(User user, UserRole userRole) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_ROLE_FROM_USER_QUERY)){
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            preparedStatement.executeUpdate();
            user.setUserRoles(getUserRolesOfUser(user));
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public boolean connectUser(User user) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(ADD_ONLINE_USER_QUERY)){
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setTime(2, new Time(new Date().getTime()));
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean disconnectUser(User user) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(DELETE_ONLINE_USER_QUERY)){
            preparedStatement.setInt(1, user.getId());
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void initialize() {
        if (connection == null) {
            try{
                connection = DriverManager.getConnection(DB_URL, "root", "cisco1");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(AUTH_USER_QUERY)){
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            try(ResultSet rs = preparedStatement.executeQuery()) {
                if(rs.next()) {
                    List<User> users = readUsers(rs);
                    if (users.size() != 1) {
                        clientHandler.sendMessage("Некорретный логин/пароль");
                        return false;
                    }
                    User user = users.get(0);
                    if (server.isUsernameBusy(user.getUsername())) {
                        clientHandler.sendMessage("Указанная учетная запись уже занята");
                        return false;
                    }
                    clientHandler.setUser(user);
                    server.subscribe(clientHandler);
                    clientHandler.sendMessage("/authok " + user.getUsername());
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username, AuthorizationRole role) {
        return false;
    }
}
