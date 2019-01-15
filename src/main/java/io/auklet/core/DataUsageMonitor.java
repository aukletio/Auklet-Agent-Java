package io.auklet.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.config.DataUsageLimit;
import io.auklet.config.DataUsageTracker;
import net.jcip.annotations.ThreadSafe;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * <p>This class handles tracking of data usage, enforcement of data usage limits, and periodic refresh
 * of data usage limit config.</p>
 */
@ThreadSafe
public final class DataUsageMonitor extends HasAgent {

    private final Object lock = new Object();
    private DataUsageLimit limit;
    private DataUsageTracker tracker;
    private boolean awaitingMonthlyReset = false;
    private int hoursSinceConfigRefresh = 0;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        super.start(agent);
        this.limit = new DataUsageLimit();
        this.limit.start(agent);
        this.tracker = new DataUsageTracker();
        this.tracker.start(agent);
        agent.scheduleRepeatingTask(this.createMonthlyDataUsageResetTask(), 0L, 1L, TimeUnit.DAYS);
        agent.scheduleRepeatingTask(this.createDataLimitConfigRefreshTask(), 0L, 1L, TimeUnit.HOURS);
    }

    /**
     * <p>Returns the data usage limit config for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public DataUsageConfig getUsageConfig() {
        synchronized (this.lock) {
            return this.limit.getConfig();
        }
    }

    /**
     * <p>Adds the input number of bytes to the current amount of bytes sent.</p>
     *
     * @param moreBytes no-op if less than 1.
     */
    public void addMoreData(int moreBytes) {
        if (moreBytes < 1) return;
        synchronized (this.lock) {
            this.tracker.addMoreData(moreBytes);
        }
    }

    /**
     * <p>Determines whether or not the given proposed payload size would exceed the data limit.</p>
     *
     * @param proposedPayloadSize the size of the payload in bytes.
     * @return {@code true} if sending this payload to the sink would exceed the data limit, {@code false}
     * otherwise.
     */
    public boolean willExceedLimit(long proposedPayloadSize) {
        if (proposedPayloadSize <= 0) return true;
        long dataLimit;
        long bytesSent;
        synchronized (this.lock) {
            bytesSent = this.tracker.getBytesSent();
            dataLimit = this.limit.getConfig().getCellularDataLimit();
        }
        return (dataLimit == 0) || (bytesSent) + proposedPayloadSize <= dataLimit;
    }

    /**
     * <p>Creates the periodic task that resets data usage once a month.</p>
     *
     * @return never {@code null}.
     */
    @NonNull private Runnable createMonthlyDataUsageResetTask() {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == limit.getConfig().getCellularPlanDate()) {
                        if (awaitingMonthlyReset) {
                            tracker.reset();
                            awaitingMonthlyReset = false;
                        }
                    } else {
                        awaitingMonthlyReset = true;
                    }
                }
            }
        };
    }

    /**
     * <p>Creates the periodic task that refreshes the limit config from the API once a day.</p>
     *
     * @return never {@code null}.
     */
    @NonNull private Runnable createDataLimitConfigRefreshTask() {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    hoursSinceConfigRefresh++;
                    if (hoursSinceConfigRefresh == 24) {
                        limit.refresh();
                        hoursSinceConfigRefresh = 0;
                    }
                }
            }
        };
    }

}