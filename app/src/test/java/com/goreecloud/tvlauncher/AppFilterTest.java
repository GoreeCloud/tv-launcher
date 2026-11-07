package com.goreecloud.tvlauncher;

import static org.junit.Assert.*;
import org.junit.Test;

public class AppFilterTest {
    @Test public void ignoresCaseAndOuterSpaces() {
        assertTrue(AppFilter.matches("YouTube", "  tube  "));
        assertTrue(AppFilter.matches("Spotify", "SPOT"));
    }
    @Test public void blankQueryMatchesAnyLabel() {
        assertTrue(AppFilter.matches("Netflix", ""));
        assertTrue(AppFilter.matches("Netflix", null));
    }
    @Test public void missingLabelAndNonMatchAreRejected() {
        assertFalse(AppFilter.matches(null, ""));
        assertFalse(AppFilter.matches("Settings", "music"));
    }
    @Test public void literalSearchDoesNotInterpretRegex() {
        assertTrue(AppFilter.matches("A+B", "+"));
        assertFalse(AppFilter.matches("Apple", "["));
    }
}
