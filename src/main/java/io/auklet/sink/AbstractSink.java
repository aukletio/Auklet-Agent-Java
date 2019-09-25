package io.auklet.sink;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.core.HasAgent;
import io.auklet.core.Datapoint;
import io.auklet.misc.Util;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;

/**
 * <p>Base class of all Auklet agent data sinks. Each implementation provides a {@link MessagePacker} that
 * is used to construct MessagePack payloads that are then sent to the underlying output in the
 * {@link MessagePacker} object (e.g. an {@code OutputStream}).</p>
 *
 * <p>Subclasses are thread-safe as long as they synchronize on {@link #msgpack}.</p>
 */
@ThreadSafe
public abstract class AbstractSink extends HasAgent implements Sink {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSink.class);
    @GuardedBy("itself") protected final MessageBufferPacker msgpack = MessagePack.newDefaultBufferPacker();

    @Override public void shutdown() {
        synchronized (this.msgpack) {
            try {
                this.msgpack.close();
            } catch (IOException e) {
                LOGGER.warn("Error while shutting down MessagePacker.", e);
            }
        }
    }

    @Override public void send(@Nullable Throwable throwable) throws AukletException {
        if (throwable == null) return;
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        // Assemble the complete message.
        synchronized (this.msgpack) {
            this.msgpack.clear();
            try {
                this.initMessage(11);
                this.msgpack
                        .packString("timestamp").packLong(System.currentTimeMillis())
                        .packString("excType").packString(throwable.getClass().getName())
                        .packString("message").packString(Util.orElse(throwable.getMessage(), ""))
                        .packString("stackTrace").packArrayHeader(stackTrace.length);
                for (StackTraceElement ste : stackTrace) {
                    int lineNumber = ste.getLineNumber();
                    this.msgpack.packMapHeader(4)
                            .packString("functionName").packString(ste.getMethodName())
                            .packString("className").packString(ste.getClassName())
                            .packString("filePath").packString(Util.orElse(ste.getFileName(), ""))
                            // Normalize all negative return values.
                            .packString("lineNumber").packInt(lineNumber < 0 ? -1 : lineNumber);
                }
                this.msgpack.flush();
            } catch (IOException e) {
                throw new AukletException("Could not assemble event message.", e);
            }
            byte[] payload = this.msgpack.toByteArray();
            if (payload == null || payload.length == 0) return;
            this.write(payload, this.getAgent().getDeviceAuth().getMqttEventsTopic());
        }
    }

    @Override public void send(@NonNull Datapoint datapoint) throws AukletException {
        if (datapoint == null) {
            throw new AukletException("datapoint cannot be null.");
        }
        synchronized (this.msgpack) {
            this.msgpack.clear();
            try {
                this.initMessage(10);
                this.msgpack
                        .packString("timestamp").packLong(System.currentTimeMillis())
                        // User defined type
                        .packString("type").packString(datapoint.getDataType())
                        .packString("payload");
                datapoint.getValue().writeTo(this.msgpack);
                this.msgpack.flush();
            } catch (IOException e) {
                throw new AukletException("Could not assemble datapoint message.", e);
            }
            byte[] payload = this.msgpack.toByteArray();
            if (payload == null || payload.length == 0) return;
            this.write(payload, this.getAgent().getDeviceAuth().getMqttDatapointsTopic());
        }
    }

    /**
     * <p>Writes the given byte array to the underlying data sink.</p>
     *
     * @param bytes the byte array, never {@code null} or empty.
     * @param topic the topic to send to, never {@code null} or empty.
     *              Implementations are not required to use this parameter.
     * @throws AukletException if the data cannot be written.
     */
    @GuardedBy("msgpack") protected abstract void write(@NonNull byte[] bytes, @NonNull String topic) throws AukletException;

    /**
     * <p>Starts assembling an Auklet-compatible MessagePack message, which is defined as a MessagePack
     * map with at least 7 elements in it.</p>
     *
     * @param mapSize the size of the map message.
     * @throws AukletException if the map size is less than 7, or if an error occurs while assembling the
     * message payload.
     */
    @GuardedBy("msgpack") private void initMessage(int mapSize) throws AukletException {
        if (mapSize < 7) throw new AukletException("Message size is too small.");
        try {
            this.msgpack.packMapHeader(mapSize)
                    .packString("id").packString(UUID.randomUUID().toString())
                    .packString("application").packString(this.getAgent().getAppId())
                    .packString("macAddressHash").packString(this.getAgent().getMacHash())
                    .packString("publicIP").packString(this.getAgent().getIpAddress())
                    .packString("systemMetrics");
            this.addSystemMetrics();
            this.msgpack.packString("agentVersion").packString(Auklet.VERSION)
                    .packString("device").packString(this.getAgent().getDeviceAuth().getClientUsername());
        } catch (IOException | IllegalArgumentException e) {
            throw new AukletException("Error while assembling msgpack payload.", e);
        }
    }

    /**
     * <p>Adds system metrics to the current position in the given MessagePacker as a map object.</p>
     *
     * @throws AukletException if an error occurs while assembling the message.
     */
    @GuardedBy("msgpack") private void addSystemMetrics() throws AukletException {
        try {
            this.msgpack.packMapHeader(4);
            this.getAgent().getPlatform().addSystemMetrics(this.msgpack);
            // Add other system metrics.
            this.msgpack.packString("outboundNetwork").packDouble(0);
            this.msgpack.packString("inboundNetwork").packDouble(0);
        } catch (IOException e) {
            throw new AukletException("Error while assembling msgpack payload.", e);
        }
    }
}
