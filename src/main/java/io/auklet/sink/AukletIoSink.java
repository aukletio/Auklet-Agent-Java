package io.auklet.sink;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.config.AukletIoBrokers;
import io.auklet.config.AukletIoCert;
import io.auklet.misc.Util;
import io.auklet.misc.AukletDaemonExecutor;
import net.jcip.annotations.ThreadSafe;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/** <p>The default Auklet data sink, which sends data to {@code auklet.io} via MQTT.</p> */
@ThreadSafe
public final class AukletIoSink extends AbstractSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletIoSink.class);
    private final Object lock = new Object();
    private AukletDaemonExecutor executorService;
    private MqttAsyncClient client;

    /**
     * <p>Constructs the underlying MQTT client.</p>
     *
     * @throws AukletException if the underlying MQTT client cannot be constructed or started, or if
     * the SSL cert/broker config cannot be obtained.
     */
    @Override public void start(@NonNull Auklet agent) throws AukletException {
        this.setAgent(agent);
        LOGGER.info("Establishing MQTT client.");
        try {
            AukletIoCert cert = new AukletIoCert();
            cert.start(agent);
            AukletIoBrokers brokers = new AukletIoBrokers();
            brokers.start(agent);
            // Workaround to ensure that MQTT client threads do not stop JVM shutdown.
            // https://github.com/eclipse/paho.mqtt.java/issues/402#issuecomment-424686340
            // MQTT threads must be daemon threads or else the JVM will hang on shutdown.
            this.executorService = new AukletDaemonExecutor(agent.getMqttThreads(), Util.createDaemonThreadFactory("AukletPahoMQTT-%d"));
            org.eclipse.paho.client.mqttv3.logging.LoggerFactory.setLogger("io.auklet.misc.PahoLogger");
            this.client = new MqttAsyncClient(brokers.getUrl(), agent.getDeviceAuth().getClientId(), new MemoryPersistence(), new TimerPingSender(), executorService);
            this.client.setCallback(this.getCallback());
            this.client.setBufferOpts(this.getDisconnectBufferOptions(agent));
            // Wait 10 seconds for connect to succeed, then give up.
            this.client.connect(this.getConnectOptions(agent, cert.getCert())).waitForCompletion(10000L);
        } catch (MqttException e) {
            this.shutdown();
            throw new AukletException("Could not initialize MQTT sink.", e);
        }
    }

    @Override protected void write(@NonNull byte[] bytes) throws AukletException {
        synchronized (this.lock) {
            try {
                MqttMessage message = new MqttMessage(bytes);
                message.setQos(1);
                int size = bytes.length;
                boolean willExceedLimit = this.getAgent().getUsageMonitor().willExceedLimit(size);
                if (!willExceedLimit) {
                    client.publish(this.getAgent().getDeviceAuth().getMqttEventsTopic(), message);
                    this.getAgent().getUsageMonitor().addMoreData(size);
                }
            } catch (MqttException e) {
                throw new AukletException("Error while publishing MQTT message.", e);
            }
        }
    }

    @Override public void shutdown() {
        synchronized (this.lock) {
            super.shutdown();
            if (this.client != null) {
                if (this.client.isConnected()) {
                    this.executorService.logCancelExceptions(false);
                    try {
                        // Wait 2 seconds for work to quiesce and 1 second for disconnect to finish.
                        this.client.disconnect(2000L).waitForCompletion(1000L);
                    } catch (MqttException e) {
                        // TODO remove this conditional after upgrading Paho to fix this
                        if (!e.getMessage().equals("Timed out waiting for a response from the server")) {
                            LOGGER.warn("Error while disconnecting MQTT client.", e);
                        }
                        try {
                            // Do not wait for work to quiesce.
                            // Wait 1ms to disconnect (effectively do not wait, but if we say 0ms
                            // it will actually wait forever).
                            this.client.disconnectForcibly(0L, 1L);
                        } catch (MqttException e2) {
                            LOGGER.warn("Error while forcibly disconnecting MQTT client.", e);
                        }
                    }
                }
                try {
                    this.client.close();
                } catch (MqttException e) {
                    LOGGER.warn("Error while closing MQTT client.", e);
                }
            }
            Util.shutdown(this.executorService);
        }
    }

    /**
     * <p>Returns the MQTT callback object used by the MQTT client.</p>
     *
     * @return never {@code null}.
     */
    @NonNull private MqttCallback getCallback() {
        return new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                LOGGER.error("Unexpected disconnect from MQTT.", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // TODO: handle messages received from the Auklet backend.
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // TODO: handle what happens when MQTT message delivery completes
            }
        };
    }

    /**
     * <p>Returns the MQTT disconnected buffer options object.</p>
     *
     * @param agent the Auklet agent reference. Never {@code null}.
     * @return never {@code null}.
     * @throws AukletException if the options object cannot be constructed, or if any argument is {@code null}.
     */
    @NonNull private DisconnectedBufferOptions getDisconnectBufferOptions(@NonNull Auklet agent) throws AukletException {
        if (agent == null) throw new AukletException("Auklet agent is null.");
        // Divide by 5KB to get amount of messages.
        long storageLimit = agent.getUsageMonitor().getUsageConfig().getStorageLimit();
        int bufferSize = (storageLimit == 0) ? 5000 : (int) storageLimit / 5000;
        DisconnectedBufferOptions disconnectOptions = new DisconnectedBufferOptions();
        disconnectOptions.setBufferEnabled(true);
        disconnectOptions.setDeleteOldestMessages(true);
        disconnectOptions.setPersistBuffer(true);
        disconnectOptions.setBufferSize(bufferSize);
        return disconnectOptions;
    }

    /**
     * <p>Returns the MQTT connect options object.</p>
     *
     * @param agent the Auklet agent reference. Never {@code null}.
     * @param cert the Auklet SSL certificate object. Never {@code null}.
     * @return never {@code null}.
     * @throws AukletException if the options object cannot be constructed, or if any argument is {@code null}.
     */
    @NonNull private MqttConnectOptions getConnectOptions(@NonNull Auklet agent, @NonNull X509Certificate cert) throws AukletException {
        if (agent == null) throw new AukletException("Auklet agent is null.");
        if (cert == null) throw new AukletException("SSL cert is null.");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(agent.getDeviceAuth().getClientUsername());
        options.setPassword(agent.getDeviceAuth().getClientPassword().toCharArray());
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        options.setSocketFactory(this.getSocketFactory(cert));
        return options;
    }

    /**
     * <p>Returns the MQTT SSL socket factory.</p>
     *
     * @param cert the Auklet SSL certificate object. Never {@code null}.
     * @return never {@code null}.
     * @throws AukletException if the factory cannot be constructed, or if any argument is {@code null}.
     */
    @NonNull private SSLSocketFactory getSocketFactory(@NonNull X509Certificate cert) throws AukletException {
        if (cert == null) throw new AukletException("SSL cert is null.");
        try {
            KeyStore ca = KeyStore.getInstance(KeyStore.getDefaultType());
            ca.load(null, null);
            ca.setCertificateEntry("ca-certificate", cert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ca);
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException e) {
            throw new AukletException("Error while setting up MQTT SSL socket factory.", e);
        }
    }

}
