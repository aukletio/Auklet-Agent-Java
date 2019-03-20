package io.auklet.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import net.jcip.annotations.ThreadSafe;

/**
 * <p>Descendants of this class have a reference to the {@link Auklet} agent.</p>
 *
 * <p>Objects that extend this class should not be considered fully constructed until {@link #start(Auklet)}
 * is successfully invoked. It is acceptable to write Javadocs, enforce null checks, and otherwise design
 * subclasses under the assumption that this method is properly invoked beforehand. Due to these
 * semantics, such objects/classes should <b>never</b> form part of the public-facing agent API.</p>
 *
 * <p>Pursuant to the above semantics, since such objects are not considered fully-constructed until
 * this method is invoked, objects should not be accessed by multiple threads until this method is
 * invoked, and any documentation regarding thread safety should be interpreted with this in mind.</p>
 *
 * <p>This class is thread-safe, but that does not guarantee that subclasses are thread-safe.</p>
 */
@ThreadSafe
public abstract class HasAgent {

    private final Object lock = new Object();
    private Auklet agent = null;

    /**
     * <p>Invokes the post-construction start logic for this object. Implementations are required to invoke
     * {@link #setAgent(Auklet)}.</p>
     *
     * @param agent the Auklet agent object. Never {@code null}.
     * @throws AukletException if the agent reference has already been set, or if the input is {@code null}.
     */
    public abstract void start(@NonNull Auklet agent) throws AukletException;

    /**
     * <p>Sets the Auklet agent reference.</p>
     *
     * @param agent the Auklet agent object. Never {@code null}.
     * @throws AukletException if the agent reference has already been set, or if the input is {@code null}.
     */
    protected final void setAgent(@NonNull Auklet agent) throws AukletException {
        if (agent == null) throw new AukletException("Auklet agent is null.");
        synchronized(this.lock) {
            if (this.agent != null) throw new AukletException("Auklet agent already set.");
            this.agent = agent;
        }
    }

    /**
     * <p>Returns the Auklet agent reference.</p>
     *
     * @return never {@code null}.
     * @throws AukletException if the agent reference has not been set.
     */
    @NonNull protected final Auklet getAgent() throws AukletException {
        synchronized(this.lock) {
            if (this.agent == null) throw new AukletException("Auklet agent not set.");
            return this.agent;
        }
    }

    /* Prevent finalizer attacks. */
    @SuppressWarnings("deprecation")
    @Override protected final void finalize() {} //NOSONAR

}
