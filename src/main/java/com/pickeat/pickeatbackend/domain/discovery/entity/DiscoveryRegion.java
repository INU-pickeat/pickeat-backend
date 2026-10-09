package com.pickeat.pickeatbackend.domain.discovery.entity;

public enum DiscoveryRegion {
    SINSA("신사"),
    HYEHWA("혜화"),
    SEOCHON("서촌"),
    HANNAM("한남"),
    JONGNO("종로");

    private final String displayName;

    DiscoveryRegion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
