package edu.io.net.command;

import java.io.Serial;
import java.io.Serializable;

/**
 * Base class for all serializable game commands exchanged between the
 * client and the server.
 * <p>
 * Each command represents a specific action or instruction to be
 * executed remotely. Commands carry a unique identifier used to
 * correlate requests with responses, detect duplicates, and track
 * lifecycle events in asynchronous communication.
 * <p>
 * Subclasses should define additional fields representing command
 * parameters or payloads. They may also override serialization logic
 * if required, although commands are intended to remain lightweight
 * and immutable.
 * <p>
 * This class implements {@link Serializable}, enabling commands to be
 * transmitted over a network or persisted for later replay.
 * <p>
 * Thread safety: instances are immutable and therefore safe to share
 * across threads.
 */
public abstract class Command implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Unique identifier for this command instance. */
    private final long id;

    /**
     * Marker interface for representing command execution status.
     * <p>
     * Implementations may define concrete result types or error
     * indicators associated with command processing. It is intentionally
     * empty to allow flexible extension by specific command workflows.
     */
    public interface Status {}

    /**
     * Constructs a new {@code Command} with an automatically generated
     * identifier.
     * <p>
     * The identifier is based on the current system time in
     * milliseconds. Although not strictly guaranteed to be unique,
     * collisions are unlikely in typical usage scenarios. If stronger
     * guarantees are required, subclasses may provide alternative ID
     * generation strategies.
     */
    public Command() {
        this.id = System.currentTimeMillis();
    }

    /**
     * Constructs a new {@code Command} with the specified identifier.
     * <p>
     * This constructor is typically used when reconstructing commands
     * received from remote peers or restoring previously persisted
     * command state.
     *
     * @param id unique identifier to assign to this command
     */
    public Command(long id) {
        this.id = id;
    }

    /**
     * Returns the unique identifier of this command.
     * <p>
     * The ID can be used to link requests with their corresponding
     * responses or for deduplication in transport layers.
     *
     * @return unique identifier of the command
     */
    public long id() {
        return id;
    }
}
