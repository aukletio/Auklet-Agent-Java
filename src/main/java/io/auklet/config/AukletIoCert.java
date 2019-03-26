package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.misc.Util;
import net.jcip.annotations.NotThreadSafe;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * <p>This config file contains the MQTT CA certificate for {@code auklet.io}.</p>
 */
@NotThreadSafe
public final class AukletIoCert extends AbstractConfigFileFromApi<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletIoCert.class);

    private X509Certificate cert;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        LOGGER.debug("Loading auklet.io CA certificate.");
        super.start(agent);
        String certString = this.loadConfig();
        // Load the cert file from disk and convert it to an X509 object.
        try (BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(certString.getBytes("UTF-8")))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            this.cert = (X509Certificate) cf.generateCertificate(bis);
        } catch (CertificateException | IOException e) {
            throw new AukletException("Could not convert PEM certificate into object format.", e);
        }
    }

    @Override public String getName() {
        return "CA";
    }

    /**
     * <p>Returns the certificate object.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public X509Certificate getCert() { return this.cert; }

    @Override protected String readFromDisk() {
        try {
            return Util.orElseNullEmpty(this.getStringFromDisk(), null);
        } catch (IOException e) {
            LOGGER.warn("Could not read cert file from disk, will re-download from API.", e);
            return null;
        }
    }

    @Override protected String fetchFromApi() throws AukletException {
        try {
            Request.Builder request = new Request.Builder()
                    .url(this.getAgent().getBaseUrl() + "/private/devices/certificates/").get();
            try (Response response = this.getAgent().getApi().doRequest(request)) {
                String responseString = response.body().string();
                if (response.isSuccessful()) {
                    return responseString;
                } else {
                    throw new AukletException(String.format("Error while getting Auklet SSL cert: %s: %s", response.message(), responseString));
                }
            }
        } catch (IOException e) {
            throw new AukletException("Error while getting Auklet SSL cert.", e);
        }
    }

    @Override protected void writeToDisk(@NonNull String contents) throws AukletException {
        if (Util.isNullOrEmpty(contents)) throw new AukletException("Input is null or empty.");
        try {
            Util.writeUtf8(this.file, contents);
        } catch (IOException e) {
            throw new AukletException("Could not save MQTT CA cert to disk.", e);
        }
    }

}
