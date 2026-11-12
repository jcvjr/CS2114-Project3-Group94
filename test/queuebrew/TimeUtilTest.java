package queuebrew;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for TimeUtil.
 */
public class TimeUtilTest {

    @Test
    public void formatsMinutesAndPaddedSeconds() {
        assertEquals("2:05", TimeUtil.formatTime(125));
        assertEquals("1:00", TimeUtil.formatTime(60));
        assertEquals("0:00", TimeUtil.formatTime(0));
        assertEquals("0:59", TimeUtil.formatTime(59));
    }

    @Test
    public void rejectsNegativeSeconds() {
        assertThrows(IllegalArgumentException.class,
            () -> TimeUtil.formatTime(-1));
    }
}
