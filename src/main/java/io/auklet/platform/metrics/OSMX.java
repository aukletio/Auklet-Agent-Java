package io.auklet.platform.metrics;

import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A frontend for {@link java.lang.management.OperatingSystemMXBean},
 * {@link com.sun.management.OperatingSystemMXBean} and {@link com.sun.management.UnixOperatingSystemMXBean}, depending
 * on which OS is being used and whether or not the JVM implements the {@code com.sun} classes.</p>
 *
 * <p>Methods in this class that return strings could, if you were to use the bean object directly, throw
 * {@link SecurityException}; instead, this class hides those exceptions and returns empty strings if this
 * occurs.</p>
 *
 * <p>Methods in this class that return numeric values may return negative values under certain circumstances
 * (see original Javadocs), and will also return negative values if the underlying method is not supported
 * because the required {@code com.sun} class is missing. If the {@code java.lang.management} classes are
 * missing, methods that return strings will return an empty string, {@link #getSystemLoadAverage()} will
 * return -1, and {@link #getAvailableProcessors()} will return 0.</p>
 *
 * <p>This is a singleton; use {@link #BEAN} to retrieve the instance. The {@link #BEAN} maintains no
 * state and is thus immutable.</p>
 */
@Immutable
public enum OSMX {

    BEAN;

    private static final Logger LOGGER = LoggerFactory.getLogger(OSMX.class);
    private static final Object realBean;
    private static final boolean EXISTS;
    private static final boolean IS_SUN;
    private static final boolean IS_SUN_UNIX;

    // Since enums do not serialize any instance fields, we use a static initializer
    // so that our (static) fields are initialized upon classload, prior to any
    // deserialization taking place.
    static {
        Object bean = null;
        boolean exists = false;
        boolean sun = false;
        boolean sunUnix = false;
        try {
            bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            exists = true;
        } catch (NoClassDefFoundError e) {
            LOGGER.warn("java.lang.management classes do not exist; system/JVM memory and CPU stats will not be available.");
        }
        if (bean != null) {
            try {
                sun = bean instanceof com.sun.management.OperatingSystemMXBean; // NOSONAR
            } catch (NoClassDefFoundError e) {
                LOGGER.warn("com.sun.management.OperatingSystemMXBean does not exist; system/JVM memory and CPU stats will not be available.");
            }
            if (sun) {
                try {
                    sunUnix = bean instanceof com.sun.management.UnixOperatingSystemMXBean; // NOSONAR
                    LOGGER.info("Running on Unix platform.");
                } catch (NoClassDefFoundError e) {
                    LOGGER.info("Running on non-Unix platform.");
                }
            }
        }
        realBean = bean;
        EXISTS = exists;
        IS_SUN = sun;
        IS_SUN_UNIX = sunUnix;
    }

    @NonNull public String getName() {
        if (!EXISTS) return "";
        try {
            return get().getName();
        } catch (SecurityException e) {
            LOGGER.warn("Cannot get OS name", e);
            return "";
        }
    }
    @NonNull public String getArch() {
        if (!EXISTS) return "";
        try {
            return get().getArch();
        } catch (SecurityException e) {
            LOGGER.warn("Cannot get OS arch", e);
            return "";
        }
    }
    @NonNull public String getVersion() {
        if (!EXISTS) return "";
        try {
            return get().getVersion();
        } catch (SecurityException e) {
            LOGGER.warn("Cannot get OS version", e);
            return "";
        }
    }
    public int getAvailableProcessors() {
        int value = 0;
        if (EXISTS) value = get().getAvailableProcessors();
        return value;
    }
    public double getSystemLoadAverage() {
        double value = -1;
        if (EXISTS) value = get().getSystemLoadAverage();
        return value;
    }
    public long getCommittedVirtualMemorySize() {
        long value = -1;
        if (IS_SUN) value = asSun().getCommittedVirtualMemorySize();
        return value;
    }
    public long getTotalSwapSpaceSize() {
        long value = -1;
        if (IS_SUN) value = asSun().getTotalSwapSpaceSize();
        return value;
    }
    public long getFreeSwapSpaceSize() {
        long value = -1;
        if (IS_SUN) value = asSun().getFreeSwapSpaceSize();
        return value;
    }
    public long getProcessCpuTime() {
        long value = -1;
        if (IS_SUN) value = asSun().getProcessCpuTime();
        return value;
    }
    public long getFreePhysicalMemorySize() {
        long value = -1;
        if (IS_SUN) value = asSun().getFreePhysicalMemorySize();
        return value;
    }
    public long getTotalPhysicalMemorySize() {
        long value = -1;
        if (IS_SUN) value = asSun().getTotalPhysicalMemorySize();
        return value;
    }
    public double getSystemCpuLoad() {
        double value = -1;
        if (IS_SUN) value = asSun().getSystemCpuLoad();
        return value;
    }
    public double getProcessCpuLoad() {
        double value = -1;
        if (IS_SUN) value = asSun().getProcessCpuLoad();
        return value;
    }
    public long getOpenFileDescriptorCount() {
        long value = -1;
        if (IS_SUN_UNIX) value = asSunUnix().getOpenFileDescriptorCount();
        return value;
    }
    public long getMaxFileDescriptorCount() {
        long value = -1;
        if (IS_SUN_UNIX) value = asSunUnix().getMaxFileDescriptorCount();
        return value;
    }

    // These methods return the realBean object cast to the appropriate type.
    // Calls to these methods must always be guarded by checking the corresponding
    // boolean flag via an if/else statement. Do not use ternary operators for this,
    // as it will cause NoClassDefFoundErrors on JVMs that lack the required packages.
    @NonNull private static java.lang.management.OperatingSystemMXBean get() {
        return (java.lang.management.OperatingSystemMXBean) realBean;
    }
    @NonNull private static com.sun.management.OperatingSystemMXBean asSun() {
        return (com.sun.management.OperatingSystemMXBean) realBean;
    }
    @NonNull private static com.sun.management.UnixOperatingSystemMXBean asSunUnix() {
        return (com.sun.management.UnixOperatingSystemMXBean) realBean;
    }

}
