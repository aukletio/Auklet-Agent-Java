package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.misc.Util;
import mjson.Json;
import net.jcip.annotations.NotThreadSafe;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * <p>This config file contains the Auklet organization ID to which the application ID belongs,
 * as well as the credentials used to authenticate to the {@code auklet.io} data pipeline.</p>
 */
@NotThreadSafe
public final class DeviceAuth extends AbstractJsonConfigFileFromApi {

    public static final String FILENAME = "AukletAuth";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAuth.class);

    private Cipher aesCipher;
    private Key aesKey;
    private String organizationId;
    private String clientId;
    private String clientUsername;
    private String clientPassword;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        LOGGER.debug("Loading device auth file.");
        super.start(agent);
        // Setup AES cipher.
        try {
            this.aesCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AukletException("Could not get AES cipher.", e);
        }
        // MQTT credentials are derived from the app ID, so use that as the encryption key.
        // This way, if the app ID changes, we will obtain new credentials.
        this.aesKey = new SecretKeySpec(agent.getAppId().substring(0,16).getBytes(), "AES");
        // Read/parse the config.
        Json config = this.loadConfig();
        this.organizationId = config.at("organization").asString();
        this.clientId = config.at("client_id").asString();
        this.clientUsername = config.at("id").asString();
        this.clientPassword = config.at("client_password").asString();
    }

    @Override public String getName() {
        return FILENAME;
    }

    /**
     * <p>Returns the organization ID for this device.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getOrganizationId() {
        return this.organizationId;
    }

    /**
     * <p>Returns the MQTT client ID for this device.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getClientId() {
        return this.clientId;
    }

    /**
     * <p>Returns the MQTT client username for this device.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getClientUsername() {
        return this.clientUsername;
    }

    /**
     * <p>Returns the MQTT client password for this device.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getClientPassword() {
        return this.clientPassword;
    }

    /**
     * <p>Returns the MQTT topic that should be used for publishing event messages.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getMqttEventsTopic() {
        return "java/events/" + this.getOrganizationId() + "/" + this.getClientUsername();
    }

    /**
     * <p>Returns the MQTT topic that hsould be used for publishing datapoint messages.</p>
     * @return never {@code null}.
     */
    @NonNull public String getMqttDatapointsTopic() {
        return "datapoints/" + this.getOrganizationId() + "/" + this.getClientUsername();
    }

    @Override protected Json readFromDisk() {
        try {
            // Read and decrypt the device auth file from disk.
            byte[] authFileBytes = Util.read(this.file);
            if (authFileBytes.length == 0) return null;
            this.aesCipher.init(Cipher.DECRYPT_MODE, this.aesKey);
            String authFileDecrypted = new String(this.aesCipher.doFinal(authFileBytes));
            // Parse the JSON and set relevant fields.
            return Util.validateJson(Util.readJson(authFileDecrypted), this.getClass().getName());
        } catch (AukletException | IOException | SecurityException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IllegalArgumentException e) {
            LOGGER.warn("Could not read device auth file from disk, will re-register device with API.", e);
            return null;
        }
    }

    @Override protected Json fetchFromApi() throws AukletException {
        Json requestJson = Json.object();
        requestJson.set("mac_address_hash", this.getAgent().getMacHash());
        requestJson.set("application", this.getAgent().getAppId());
        Request.Builder request = new Request.Builder()
                .url(this.getAgent().getBaseUrl() + "/private/devices/")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestJson.toString()))
                .header("Content-Type", "application/json; charset=utf-8");
        return this.makeJsonRequest(request);
    }

    @Override protected void writeToDisk(@NonNull Json contents) throws AukletException {
        if (contents == null) throw new AukletException("Input is null.");
        try {
            // Encrypt and save the JSON string to disk.
            this.aesCipher.init(Cipher.ENCRYPT_MODE, this.aesKey);
            byte[] encrypted = this.aesCipher.doFinal(contents.toString().getBytes("UTF-8"));
            Util.write(this.file, encrypted);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new AukletException("Could not encrypt/save device data to disk.", e);
        }
    }

}
