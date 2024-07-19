package ru.otus.june.chat.server.providers.jdbc;

import ru.otus.june.chat.server.providers.AuthenticationProvider;
import ru.otus.june.chat.server.providers.jdbc.entity.User;
import ru.otus.june.chat.server.providers.jdbc.entity.UserActivity;
import ru.otus.june.chat.server.providers.jdbc.entity.UserRole;

import java.util.List;

public interface UserService extends AuthenticationProvider {
    List<User> getAllUsers();

    User getUser(String name, boolean byLogin);

    boolean addUser(User user);

    boolean updateUser(User user, boolean updateUserRoles);

    boolean deleteUser(User user);

    UserRole getUserRole(String userRoleName);

    List<UserRole> getUserRoles();

    boolean addUserRole(UserRole userRole);

    List<UserRole> getUserRolesOfUser(User user);

    boolean isUserRoleOfUser(User user, UserRole userRole);

    boolean addUserRoleToUser(User user, UserRole userRole);

    boolean removeUserRoleFromUser(User user, UserRole userRole);

    UserActivity getUserActivity(User user);

    boolean addUserActivity(UserActivity userActivity);

    boolean updateUserActivity(UserActivity userActivity);

    void connectUser(User user);

    void disconnectUser(User user, boolean isKicked);
}
