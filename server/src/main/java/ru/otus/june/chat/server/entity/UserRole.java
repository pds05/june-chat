package ru.otus.june.chat.server.entity;

public class UserRole {
    private int id;
    private String authRole;
    private String description;

    public UserRole(int id, String authRole, String description) {
        this.id = id;
        this.authRole = authRole;
        this.description = description;
    }

    public UserRole(String authRole, String description) {
        this.authRole = authRole;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAuthRole() {
        return authRole;
    }

    public void setAuthRole(String authRole) {
        this.authRole = authRole;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", authRole='" + authRole + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
