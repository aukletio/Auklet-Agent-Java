package io.auklet.sink;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;

/** <p>A location to which the Auklet agent will write data.</p> */
public interface Sink {

    /**
     * <p>Transforms the given throwable object into an Auklet event object and sends it to this sink.</p>
     *
     * @param throwable if {@code null}, this method is no-op.
     * @throws AukletException if an error occurs while sending the event to the sink.
     */
    void send(@Nullable Throwable throwable) throws AukletException;

    /**
     * <p>Shuts down this data sink and disconnects/closes any underlying resources.</p>
     *
     * @throws AukletException if an error occurs while closing the sink or its underlying resources.
     */
    void shutdown() throws AukletException;

}
