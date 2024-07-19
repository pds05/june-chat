package ru.otus.june.chat.server.providers.jdbc.entity;

import java.util.Date;
import java.util.List;

public class User {
    private Integer id;
    private String username;
    private String login;
    private String password;
    private String email;
    private long phoneNumber;
    private boolean isActive;
    private Date registrationDate;
    private Date deactivationDate;
    private List<UserRole> userRoles;
    private UserActivity activity;

    public User(Integer id, String username, String login, String password, String email, long phoneNumber, boolean isActive, Date registrationDate, Date deactivationDate) {
        this.id = id;
        this.username = username;
        this.login = login;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isActive = isActive;
        this.registrationDate = registrationDate;
        this.deactivationDate = deactivationDate;
    }

    public User(String username, String login, String password, String email, long phoneNumber, boolean isActive, Date registrationDate) {
        this.username = username;
        this.login = login;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isActive = isActive;
        this.registrationDate = registrationDate;
    }

    public User(){};

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(long phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Date getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(Date deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public UserActivity getActivity() {
        return activity;
    }

    public void setActivity(UserActivity activity) {
        this.activity = activity;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber=" + phoneNumber +
                ", isActive=" + isActive +
                ", registrationDate=" + registrationDate +
                ", deactivationDate=" + deactivationDate +
                ", userRoles=" + userRoles +
                ", activity=" + activity +
                '}';
    }
}
