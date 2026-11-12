package queuebrew;

/**
 * Static helpers for showing time.
 */
public class TimeUtil {

    private TimeUtil() {
        // static helpers only
    }

    /**
     * Converts seconds into countdown text such as "2:05".
     *
     * @param totalSeconds
     *            the number of seconds
     * @return minutes and zero-padded seconds
     * @throws IllegalArgumentException
     *             if totalSeconds is negative
     */
    public static String formatTime(int totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("seconds must not be negative");
        }
        return (totalSeconds / 60) + ":" + String.format("%02d", totalSeconds % 60);
    }
}
