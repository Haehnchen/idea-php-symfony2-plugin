package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.util.ModificationTracker;

import java.time.Instant;

/**
 * Provide a modification on nearest second value
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TimeSecondModificationTracker implements ModificationTracker {

    public static final ModificationTracker TIMED_MODIFICATION_TRACKER_60 = new TimeSecondModificationTracker(60);

    private final int expiresAfter;

    public TimeSecondModificationTracker(int expiresAfter) {
        this.expiresAfter = expiresAfter;
    }

    @Override
    public long getModificationCount() {
        long unixTime = Instant.now().getEpochSecond();
        return roundNearest(unixTime);
    }

    private long roundNearest(long n)  {
        // Smaller multiple
        long a = (n / this.expiresAfter) * this.expiresAfter;

        // Larger multiple
        long b = a + this.expiresAfter;

        // Return of closest of two
        return (n - a > b - n) ? b : a;
    }
}
