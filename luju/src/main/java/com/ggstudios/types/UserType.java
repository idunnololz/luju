package com.ggstudios.types;

public class UserType {
    private String type;

    public UserType (String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
