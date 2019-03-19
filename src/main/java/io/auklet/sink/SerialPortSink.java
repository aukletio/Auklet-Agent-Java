package io.auklet.sink;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.Auklet;
import io.auklet.AukletException;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;

import java.io.IOException;
import java.io.OutputStream;

/** <p>An Auklet data sink backed by a named serial port.</p> */
@ThreadSafe
public final class SerialPortSink extends AbstractSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortSink.class);
    private final Object lock = new Object();
    private SerialPort port;
    private OutputStream out;

    /**
     * <p>Constructs the serial data sink and opens the underlying serial port.</p>
     *
     * @throws AukletException if the serial port does not exist or is already in use, or if the
     * serial port's underlying output stream cannot be obtained.
     */
    @Override public void start(@NonNull Auklet agent) throws AukletException {
        this.setAgent(agent);
        String portName = this.getAgent().getSerialPort();
        LOGGER.info("Connecting to serial port: {}", portName);
        try {
            String appName = "auklet:" + this.getAgent().getAppId();
            this.port = (SerialPort) CommPortIdentifier.getPortIdentifier(portName).open(appName, 1000);
        } catch (NoSuchPortException | PortInUseException e) {
            this.shutdown();
            throw new AukletException("Could not initialize serial port sink.", e);
        }
        try {
            this.out = this.port.getOutputStream();
        } catch (IOException e) {
            this.shutdown();
            throw new AukletException("Could not initialize serial port sink.", e);
        }
    }

    /**
     * <p>Sends the given throwable as an Auklet event object, wrapped further inside another MessagePack
     * map with 2 elements: the name of the target MQTT topic and the event payload.</p>
     */
    @Override public void send(@Nullable Throwable throwable) throws AukletException {
        synchronized (this.lock) {
            synchronized (this.msgpack) {
                try {
                    LOGGER.debug("Adding MQTT info to payload.");
                    this.msgpack.packMapHeader(2)
                            .packString("topic").packString(this.getAgent().getDeviceAuth().getMqttEventsTopic())
                            .packString("payload"); // The value will be set by calling super.write().
                } catch (IOException e) {
                    throw new AukletException("Could not assemble event message.", e);
                }
                super.send(throwable);
            }
        }
    }

    @Override protected void write(@NonNull byte[] bytes) throws AukletException {
        try {
            int size = bytes.length;
            boolean willExceedLimit = this.getAgent().getUsageMonitor().willExceedLimit(size);
            if (!willExceedLimit) {
                this.out.write(bytes);
                this.out.flush();
                this.getAgent().getUsageMonitor().addMoreData(size);
            }
        } catch (IOException e) {
            throw new AukletException("Could not write data to serial port", e);
        }
    }

    @Override public void shutdown() {
        synchronized (this.lock) {
            super.shutdown();
            if (this.port != null) this.port.close(); // implicitly closes this.out
        }
    }

}
