package ru.otus.june.chat.server;

import ru.otus.june.chat.server.entity.User;
import ru.otus.june.chat.server.entity.UserRole;

import java.util.List;

public interface UserService extends AuthenticationProvider {
    List<User> getAllUsers();
    User getUser(String username);
    User addUser(User user);
    boolean updateUser(User user);
    boolean deleteUser(User user);

    List<UserRole> getUserRoles();
    List<UserRole> getUserRolesOfUser(User user);
    UserRole addUserRole(UserRole userRole);
    boolean isUsersUserRole(User user, UserRole userRole);

    User addUserRoleToUser(User user, UserRole userRole);
    User removeUserRoleFromUser(User user, UserRole userRole);

    boolean connectUser(User user);
    boolean disconnectUser(User user);
}
