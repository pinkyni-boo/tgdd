package com.hutech.demo;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AppRole {
    ADMIN(1),
    MANAGER(2),
    USER(3);

    public final long value;
}
