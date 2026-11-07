package com.qmz.canteen.security;

public enum UserRole {

    STUDENT("学生"),
    ADMIN("食堂管理员"),
    MERCHANT("商家");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
