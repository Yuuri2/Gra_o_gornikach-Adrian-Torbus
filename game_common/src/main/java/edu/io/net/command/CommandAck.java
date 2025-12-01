package edu.io.net.command;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * Represents an acknowledgement command sent in response to the
 * reception of another {@link Command}.
 * <p>
 * A {@code CommandAck} confirms that a specific command was received
 * and optionally includes a response command that may contain the
 * result of its processing.
 * <p>
 * This class is immutable and extends {@link Command}, reusing the
 * identifier of the acknowledged request. This allows the sender to
 * correlate acknowledgements with their original commands.
 * <p>
 * The request command is marked as {@code transient} since it is
 * typically used only in local application logic and is not meant to
 * be serialized over the network.
 */
public class CommandAck extends Command {

    /**
     * A special {@code CommandAck} instance representing the absence of
     * a meaningful acknowledgement.
     *
     * <p>{@code NO_ACK} can be returned when a command is unknown,
     * unsupported, or when no response should be sent to the client.
     * This avoids sending null or throwing exceptions while still
     * fulfilling the contract of returning a {@link CommandAck}.
     *
     * <p>Since {@code NO_ACK} is immutable and shared, it can be safely
     * reused across the server without creating new objects.
     */
    public static final CommandAck NO_ACK =
            new CommandAck(new CommandRe(CommandRe.Status.OK) {}) {};

    /** The original command being acknowledged.
     * <p>
     * This field is {@code transient} and will not be serialized,
     * as responses are often processed locally and are not part of
     * the persisted or transmitted data stream.
     */
    private final transient Command reqCmd;

    /**
     * The response command associated with the acknowledgement.
     */
    private final CommandRe resCmd;

    /**
     * Constructs a new acknowledgement for the specified request
     * command without an associated response.
     * <p>
     * Equivalent to calling:
     * <pre>{@code
     * new CommandAck(reqCmd, Command.NO_CMD);
     * }</pre>
     *
     * @param reqCmd command being acknowledged
     * @throws NullPointerException if {@code reqCmd} is {@code null}
     */
    public CommandAck(@NotNull Command reqCmd) {
        this(reqCmd, CommandRe.NO_CMD);
    }

    /**
     * Constructs a new acknowledgement for the specified request and
     * response commands.
     * <p>
     * The acknowledgement inherits the identifier of the request
     * command, allowing correlation between them.
     *
     * @param reqCmd command being acknowledged
     * @param resCmd response command associated with the acknowledgement
     * @throws NullPointerException if either argument is {@code null}
     */
    public CommandAck(
            @NotNull Command reqCmd,
            @NotNull CommandRe resCmd) {
        super(reqCmd.id());
        this.reqCmd = Objects.requireNonNull(reqCmd);
        this.resCmd = Objects.requireNonNull(resCmd);
    }

    /**
     * Returns the original command that this acknowledgement refers
     * to.
     *
     * @return acknowledged request command
     */
    public Command reqCmd() {
        return reqCmd;
    }

    /**
     * Returns the response command associated with this
     * acknowledgement.
     * <p>
     * May be a placeholder command (e.g. {@code Command.NO_CMD})
     * if no response is available.
     *
     * @return associated response command
     */
    public CommandRe resCmd() {
        return resCmd;
    }
}
