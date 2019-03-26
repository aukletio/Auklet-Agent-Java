package io.auklet.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.config.DataUsageLimit;
import io.auklet.config.DataUsageTracker;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * <p>This class handles tracking of data usage, enforcement of data usage limits, and periodic refresh
 * of the {@link DataUsageLimit} config.</p>
 *
 * <p>The enforcement of the data usage limit provided by this class is fuzzy by nature; due to a lack
 * of OS integration to sniff all traffic that's being sent across the wire, this class will always
 * underestimate the amount of data being sent by the agent. End-users must be careful to configure
 * their usage limits with some expected overhead in mind.</p>
 */
@ThreadSafe
public final class DataUsageMonitor extends HasAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataUsageMonitor.class);
    private final Object lock = new Object();
    private DataUsageLimit limit;
    private DataUsageTracker tracker;
    private boolean awaitingMonthlyReset = false;
    private int hoursSinceConfigRefresh = 0;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        LOGGER.info("Starting data usage monitor service.");
        this.setAgent(agent);
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
        LOGGER.debug("Recording more sinked data: {}", moreBytes);
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
        LOGGER.debug("Testing data limit for payload size: {}", proposedPayloadSize);
        long dataLimit;
        long bytesSent;
        synchronized (this.lock) {
            bytesSent = this.tracker.getBytesSent();
            dataLimit = this.limit.getConfig().getCellularDataLimit();
        }
        boolean result = false;
        if (dataLimit > 0 && proposedPayloadSize > 0) result = bytesSent + proposedPayloadSize <= dataLimit;
        LOGGER.debug("Data limit test result: {}", result);
        return result;
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
                            LOGGER.info("Reset monthly data usage tracker.");
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
                        LOGGER.info("Refreshed data limit config from API.");
                    }
                }
            }
        };
    }

}
