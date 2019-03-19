package io.auklet;

/**
 * <p>Superclass of all exceptions unique to the Auklet agent.</p>
 *
 * <p>The <b>only</b> classes/methods in the Auklet agent Javadocs that are officially supported for end
 * users are:</p>
 *
 * <ul>
 *   <li>All {@code public static} methods in the {@link Auklet} class.</li>
 *   <li>All {@code public} methods in the {@link Config} class.</li>
 *   <li>The {@link AukletException} class.</li>
 * </ul>
 *
 * <p><b>Unless instructed to do so by Auklet support, do not use any classes/fields/methods other than
 * those described above.</b></p>
 */
public final class AukletException extends Exception {

    public static final long serialVersionUID = 0L;

    public AukletException() {}

    public AukletException(String message) { super(message); }

    public AukletException(String message, Throwable cause) { super(message, cause); }

    public AukletException(Throwable cause) { super(cause); }

}
