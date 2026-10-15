package com.sumirelabs.sum1rem0d.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrowserLayoutTest {
    @Test void fitsLeftOfVanillaWithoutChangingContainerPosition() {
        var layout = BrowserLayout.fit(250, 360, 180, 180, 8);
        assertEquals(62, layout.x()); assertEquals(180, layout.width());
        assertTrue(layout.visible()); assertTrue(layout.x() + layout.width() < 250);
    }
    @Test void smallerWindowsShrinkAndVeryNarrowOnesHideThePanel() {
        var layout = BrowserLayout.fit(90, 240, 180, 180, 8);
        assertEquals(78, layout.width()); assertEquals(4, layout.x());
        assertFalse(BrowserLayout.fit(30, 100, 180, 180, 8).visible());
        assertFalse(BrowserLayout.fit(250, 60, 180, 180, 8).visible());
    }
    @Test void configuredGapMovesBrowserLeftAndKeepsExclusionOutsideSlots() {
        var layout = BrowserLayout.fit(300, 360, 180, 180, 24);
        assertEquals(96, layout.x());
        int[] exclusion = layout.exclusion();
        assertTrue(exclusion[0] + exclusion[2] < 300);
    }
}
