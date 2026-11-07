package com.goreecloud.tvlauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps all launchable apps in the single Your Apps rail.
 * A user's local priority choice affects order, never app visibility.
 */
final class AppDockOrder {
    private AppDockOrder() { }

    // A simple local interface avoids java.util.function.Predicate's API 24
    // requirement, retaining the app's current Android 6.0 / API 23 minimum.
    interface PriorityTest<T> {
        boolean isPrioritized(T item);
    }

    static <T> List<T> pinnedFirst(List<T> items, PriorityTest<? super T> isPrioritized) {
        List<T> prioritized = new ArrayList<>();
        List<T> regular = new ArrayList<>();
        for (T item : items) {
            if (isPrioritized.isPrioritized(item)) prioritized.add(item);
            else regular.add(item);
        }
        prioritized.addAll(regular);
        return prioritized;
    }
}
