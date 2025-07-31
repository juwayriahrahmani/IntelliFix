package com.loganalyzer.entity;

public enum Role {
    ADMIN("ADMIN"),
    ANALYST("ANALYST"),
    VIEWER("VIEWER");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}