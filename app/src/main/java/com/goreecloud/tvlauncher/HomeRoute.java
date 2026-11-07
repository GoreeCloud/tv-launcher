package com.goreecloud.tvlauncher;

/** Pure classification of the observed Home route versus the Android HOME role. */
final class HomeRoute {
    enum Status {
        EFFECTIVE_HOME,
        ROLE_GRANTED_BUT_SYSTEM_ROUTED,
        ROLE_GRANTED_ROUTE_UNKNOWN,
        HOME_NOT_SELECTED
    }

    private HomeRoute() { }

    static Status classify(String ourPackage, String resolvedPackage, boolean roleHeld) {
        if (ourPackage != null && ourPackage.equals(resolvedPackage)) {
            return Status.EFFECTIVE_HOME;
        }
        if (roleHeld && resolvedPackage != null) {
            return Status.ROLE_GRANTED_BUT_SYSTEM_ROUTED;
        }
        if (roleHeld) {
            return Status.ROLE_GRANTED_ROUTE_UNKNOWN;
        }
        return Status.HOME_NOT_SELECTED;
    }
}
