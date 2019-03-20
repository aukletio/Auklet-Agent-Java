package io.auklet.misc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import mjson.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** <p>Utility methods.</p> */
public final class Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    private static final String UTF_8 = "UTF-8";

    private Util() {}

    /**
     * <p>Returns whether or not a string is either null or empty.</p>
     *
     * @param s the string to check.
     * @return {@code s == null || s.isEmpty()}
     */
    public static boolean isNullOrEmpty(@Nullable String s) {
        return s == null || s.isEmpty();
    }

    /**
     * <p>Returns the first argument if it is not {@code null}, else returns the second argument.</p>
     *
     * @param a the first argument.
     * @param b the second argument.
     * @return {@code a == null ? b : a}.
     */
    @CheckForNull public static String orElse(@Nullable String a, @Nullable String b) {
        return a == null ? b : a;
    }

    /**
     * <p>Returns the first argument if it is neither {@code null} nor empty, else returns the second argument.</p>
     *
     * @param a the first argument.
     * @param b the second argument.
     * @return {isNullOrEmpty(a) ? b : a}.
     */
    @CheckForNull public static String orElseNullEmpty(@Nullable String a, @Nullable String b) {
        return isNullOrEmpty(a) ? b : a;
    }

    /**
     * <p>Removes the trailing slash from the input string, if it has one.</p>
     *
     * @param s the input string.
     * @return {@code null} if and only if the input string is {@code null}.
     */
    @CheckForNull public static String removeTrailingSlash(@Nullable String s) {
        if (s == null) return null;
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * <p>Deletes the given file and logs any exceptions that occur.</p>
     *
     * @param file no-op if {@code null}.
     */
    public static void deleteQuietly(@Nullable File file) {
        if (file == null) return;
        try { file.delete(); } // NOSONAR
        catch (SecurityException e) { /* Be quiet. */ }
    }

    /**
     * <p>Writes a string to a file using the UTF-8 charset.</p>
     *
     * @param file the file to write. No-op if {@code null}.
     * @param contents the string to write to the file. No-op if {@code null} or empty.
     */
    public static void writeUtf8(@Nullable File file, @Nullable String contents) {
        if (file == null) return;
        if (isNullOrEmpty(contents)) return;
        try {
            write(file, contents.getBytes(UTF_8));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Impossible UnsupportedEncodingException, please report this bug.", e);
        }
    }

    /**
     * <p>Writes a byte array to a file.</p>
     *
     * @param file the file to write. No-op if {@code null}.
     * @param bytes the bytes to write to the file. No-op if {@code null} or empty.
     */
    public static void write(@Nullable File file, @Nullable byte[] bytes) {
        if (file == null) return;
        if (bytes == null || bytes.length == 0) return;
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            LOGGER.warn("Could not write file.", e);
        }
    }

    /**
     * <p>Reads a file and returns its bytes.</p>
     *
     * @param file the file to read.
     * @return never {@code null}. If file is {@code null} or does not exist, returned array is empty.
     * @throws IOException if an error occurs while reading the file.
     */
    @NonNull public static byte[] read(@Nullable File file) throws IOException {
        if (file == null || file.length() == 0) return new byte[0];
        if (file.length() > Integer.MAX_VALUE) throw new IOException("File too large: " + file.length());
        byte[] bytes = new byte[(int) file.length()];
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            stream.readFully(bytes);
        }
        return bytes;
    }

    /**
     * <p>Returns a string value, falling back on an environment variable or JVM system property.</p>
     *
     * @param fromThisObj this function returns this object if it is not {@code null}.
     * @param envVar if {@code fromThisObj} is {@code null} and the environment variable named by
     * this parameter is set, this function returns the value of that environment variable. If this
     * parameter is {@code null} or empty, no environment variable is checked.
     * @param sysProp if {@code fromThisObj} is {@code null}, and if {@code envVar} is {@code null},
     * empty, or refers to an environment variable that is not set, and the JVM system property named
     * by this parameter is set, this function returns the value of that JVM system property. If this
     * parameter is {@code null} or empty, no JVM system property is checked.
     * @return possibly {@code null}.
     */
    @CheckForNull public static String getValue(@Nullable String fromThisObj, @Nullable String envVar, @Nullable String sysProp) {
        if (fromThisObj != null) return fromThisObj;
        String fromEnv = null;
        try {
            if (!isNullOrEmpty(envVar)) fromEnv = System.getenv(envVar);
        } catch (SecurityException e) {
            LOGGER.warn("Could not get env var '{}'.", envVar, e);
        }
        String fromProp = null;
        try {
            if (!isNullOrEmpty(sysProp)) fromProp = System.getProperty(sysProp);
        } catch (SecurityException e) {
            LOGGER.warn("Could not get JVM sys prop '{}'.", sysProp, e);
        }
        return orElse(fromEnv, fromProp);
    }

    /**
     * <p>Returns a boolean value, falling back on an environment variable or JVM system property.</p>
     *
     * @param fromThisObj this function returns this object if it is not {@code null}.
     * @param envVar if {@code fromThisObj} is {@code null} and the environment variable named by
     * this parameter is set, this function returns the value of that environment variable. If this
     * parameter is {@code null} or empty, no environment variable is checked.
     * @param sysProp if {@code fromThisObj} is {@code null}, and if {@code envVar} is {@code null},
     * empty, or refers to an environment variable that is not set, and the JVM system property named
     * by this parameter is set, this function returns the value of that JVM system property. If this
     * parameter is {@code null} or empty, no JVM system property is checked.
     * @return whatever boolean value is determined by the logic described above, or {@code null}
     * if all above described options fail to produce a value.
     */
    @CheckForNull public static Boolean getValue(@Nullable Boolean fromThisObj, @Nullable String envVar, @Nullable String sysProp) {
        if (fromThisObj != null) return fromThisObj;
        String stringValue = getValue((String) null, envVar, sysProp);
        return stringValue == null ? null : Boolean.valueOf(stringValue);
    }

    /**
     * <p>Returns an integer value, falling back on an environment variable or JVM system property.</p>
     *
     * @param fromThisObj this function returns this object if it is not {@code null}.
     * @param envVar if {@code fromThisObj} is {@code null} and the environment variable named by
     * this parameter is set, this function returns the value of that environment variable. If this
     * parameter is {@code null} or empty, no environment variable is checked.
     * @param sysProp if {@code fromThisObj} is {@code null}, and if {@code envVar} is {@code null},
     * empty, or refers to an environment variable that is not set, and the JVM system property named
     * by this parameter is set, this function returns the value of that JVM system property. If this
     * parameter is {@code null} or empty, no JVM system property is checked.
     * @return whatever boolean value is determined by the logic described above, or {@code null}
     * if all above described options fail to produce a value.
     */
    @CheckForNull public static Integer getValue(@Nullable Integer fromThisObj, @Nullable String envVar, @Nullable String sysProp) {
        if (fromThisObj != null) return fromThisObj;
        String stringValue = getValue((String) null, envVar, sysProp);
        if (stringValue == null) return null;
        try {
            return Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * <p>Returns a thread factory that produces daemon threads.</p>
     *
     * @param nameFormat the name format for threads created by this factory. If {@code null},
     * thread names will be autogenerated by the JVM. If not {@code null}, you can use the placeholder
     * {@code %d} to substitute the thread number (auto-increments starting at 1).
     * @return never {@code null}.
     */
    @NonNull public static ThreadFactory createDaemonThreadFactory(@Nullable final String nameFormat) {
        return new ThreadFactory() {
            private final AtomicLong counter = new AtomicLong();
            @Override public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                final long count = counter.incrementAndGet();
                if (nameFormat != null) t.setName(String.format(nameFormat, count));
                return t;
            }
        };
    }

    /**
     * <p>Parses the given JSON string into a JSON object.</p>
     *
     * @param json the string to parse. If {@code null} or empty, the returned JSON object
     * will be empty.
     * @return never {@code null}, but may be a representation of the JSON "null" element.
     */
    @NonNull public static Json readJson(@Nullable String json) {
        return Json.read(orElseNullEmpty(json, "{}"));
    }

    /**
     * <p>Validates the given JSON config object against the schema defined for the given Java class.</p>
     *
     * @param json never {@code null}.
     * @param clazz the classname of the JSON schema. Never {@code null}.
     * @return the input JSON object.
     * @throws AukletException if schema validation fails.
     */
    @NonNull public static Json validateJson(@NonNull Json json, @NonNull String clazz) throws AukletException {
        if (json == null) throw new AukletException("Input is null");
        Json schemaValidation = getJsonSchema(clazz).validate(json);
        if (schemaValidation.is("ok", true)) return json;
        List<Json> errors = schemaValidation.at("errors").asJsonList();
        StringBuilder errorString = new StringBuilder();
        for (Json error : errors) {
            errorString.append('\n').append(error.asString());
        }
        throw new AukletException(String.format("Errors while parsing Auklet JSON config file '%s': %s", clazz, errorString.toString()));
    }

    /**
     * <p>Returns the JSON schema for the given Java class.</p>
     *
     * @param clazz the classname of the JSON schema. Never {@code null}.
     * @return never {@code null}.
     * @throws AukletException if the schema could not be read.
     */
    @NonNull public static Json.Schema getJsonSchema(@NonNull String clazz) throws AukletException {
        if (clazz == null) throw new AukletException("Schema class is null.");
        String schemaPath = clazz.replace('.', '/') + ".schema.json";
        try (InputStream schemaStream = Util.class.getClassLoader().getResourceAsStream(schemaPath)) {
            if (schemaStream == null) throw new AukletException("JSON schema stream is null.");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = schemaStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return Json.schema(Json.read(new String(buffer.toByteArray(), UTF_8)));
        } catch (SecurityException | IOException e) {
            throw new AukletException("Could not read JSON schema.", e);
        }
    }

    /**
     * <p>Returns the MD5 hash of the MAC address of the first non-loopback network interface that is found by
     * this method.</p>
     *
     * @return never {@code null}. If no such interface can be found, or if its MAC address cannot be
     * read, or if this JVM does not support the MD5 algorithm, an empty string is returned.
     */
    @NonNull public static String getMacAddressHash() {
        try {
            // Find the first non-loopback interface on the system with an accessible hardware (MAC) address.
            NetworkInterface networkInterface = null;
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            while (n.hasMoreElements()) {
                NetworkInterface e = n.nextElement();
                if (!e.isLoopback() && e.getHardwareAddress() != null) {
                    networkInterface = e;
                    break;
                }
            }
            if (networkInterface == null) {
                LOGGER.warn("Could not find a non-loopback interface with an available MAC address.");
                return "";
            }
            // Convert bytes of hardware address into human-readable form.
            byte[] mac = networkInterface.getHardwareAddress();
            StringBuilder humanReadable = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                humanReadable.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            // Hash the human-readable address using MD5.
            byte[] humanReadableBytes = humanReadable.toString().getBytes(UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(humanReadableBytes);
            // Convert bytes of hashed hardware address into a hexadecimal string.
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hashBytes.length; i++) {
                hexString.append(String.format("%x", hashBytes[i]));
            }
            return hexString.toString();
        } catch (SocketException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            LOGGER.warn("Error while calculating MAC address hash.", e);
            return "";
        }
    }

    /**
     * <p>Gets the public IP address of the machine upon which this JVM is running, via
     * {@code http://checkip.amazonaws.com}.</p>
     *
     * @return never {@code null}. If an error occurs, it is logged and this function returns an empty string.
     */
    @NonNull public static String getIpAddress() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("https://checkip.amazonaws.com").openStream()))) {
            return orElse(in.readLine(), "");
        } catch (IOException e) {
            LOGGER.warn("Could not get public IP address.", e);
            return "";
        }
    }

    /**
     * <p>Shuts down the given executor service. If it does not shut down within 3 seconds,
     * or if the current thread is interrupted while waiting for shutdown to complete,
     * {@link ExecutorService#shutdownNow()} is invoked.</p>
     *
     * @param es no-op if {@code null}.
     */
    public static void shutdown(@Nullable ExecutorService es) {
        if (es == null) return;
        try {
            es.shutdown();
            if (!es.awaitTermination(3, TimeUnit.SECONDS)) es.shutdownNow();
        } catch (InterruptedException ie) {
            // End-users that call shutdown() explicitly should only do so inside the context of a JVM shutdown.
            // Thus, rethrowing this exception creates unnecessary noise and clutters the API/Javadocs.
            LOGGER.warn("Interrupted while awaiting ExecutorService shutdown.", ie);
            es.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (SecurityException se) {
            LOGGER.warn("Could not shut down ExecutorService.", se);
        }
    }

}
