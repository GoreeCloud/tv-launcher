package com.goreecloud.tvlauncher;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class HomeRouteTest {
    private static final String OUR_APP = "com.goreecloud.tvlauncher";
    private static final String OEM_APP = "com.google.android.apps.tv.launcherx";

    @Test public void grantedRoleMayStillResolveToOemLauncher() {
        assertEquals(HomeRoute.Status.ROLE_GRANTED_BUT_SYSTEM_ROUTED,
            HomeRoute.classify(OUR_APP, OEM_APP, true));
    }

    @Test public void effectiveHomeIsExplicitlyVerified() {
        assertEquals(HomeRoute.Status.EFFECTIVE_HOME,
            HomeRoute.classify(OUR_APP, OUR_APP, true));
        assertEquals(HomeRoute.Status.EFFECTIVE_HOME,
            HomeRoute.classify(OUR_APP, OUR_APP, false));
    }

    @Test public void unresolvedHomeWithHeldRoleIsNotCalledEffective() {
        assertEquals(HomeRoute.Status.ROLE_GRANTED_ROUTE_UNKNOWN,
            HomeRoute.classify(OUR_APP, null, true));
    }

    @Test public void noRoleAndDifferentResolverMeansNotSelected() {
        assertEquals(HomeRoute.Status.HOME_NOT_SELECTED,
            HomeRoute.classify(OUR_APP, OEM_APP, false));
    }

    @Test public void nullInputsNeverClaimEffectiveHome() {
        assertEquals(HomeRoute.Status.HOME_NOT_SELECTED,
            HomeRoute.classify(null, null, false));
        assertEquals(HomeRoute.Status.ROLE_GRANTED_ROUTE_UNKNOWN,
            HomeRoute.classify(OUR_APP, null, true));
    }
}
