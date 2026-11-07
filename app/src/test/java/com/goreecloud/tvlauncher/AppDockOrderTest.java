package com.goreecloud.tvlauncher;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AppDockOrderTest {
    private final List<String> alphabetical =
        Arrays.asList("Browser", "Disney", "Netflix", "YouTube");

    @Test public void emptyPreferenceKeepsAllAppsInOriginalOrder() {
        assertEquals(alphabetical, AppDockOrder.pinnedFirst(alphabetical, item -> false));
    }

    @Test public void prioritizedAppsAppearFirstWithoutHidingUnpinnedApps() {
        List<String> result = AppDockOrder.pinnedFirst(
            alphabetical, item -> item.equals("Netflix") || item.equals("YouTube"));
        assertEquals(Arrays.asList("Netflix", "YouTube", "Browser", "Disney"), result);
        assertEquals(alphabetical.size(), result.size());
    }

    @Test public void orderIsStableWithinBothGroups() {
        assertEquals(Arrays.asList("Browser", "Netflix", "Disney", "YouTube"),
            AppDockOrder.pinnedFirst(alphabetical,
                item -> item.equals("Browser") || item.equals("Netflix")));
    }

    @Test public void emptyAppListStaysEmpty() {
        assertTrue(AppDockOrder.pinnedFirst(Collections.<String>emptyList(),
            item -> true).isEmpty());
    }

    @Test public void inputOrderIsNotMutated() {
        AppDockOrder.pinnedFirst(alphabetical, item -> item.equals("YouTube"));
        assertEquals(Arrays.asList("Browser", "Disney", "Netflix", "YouTube"), alphabetical);
    }
}
