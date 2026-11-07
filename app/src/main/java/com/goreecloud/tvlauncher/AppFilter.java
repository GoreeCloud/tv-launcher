package com.goreecloud.tvlauncher;

import java.util.Locale;

/** Locale-stable, case-insensitive on-device app search. */
public final class AppFilter {
    private AppFilter() { }

    public static boolean matches(String label, String query) {
        if (label == null) return false;
        if (query == null || query.trim().isEmpty()) return true;
        return label.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }
}
