package edu.io.net.command;

import java.util.Objects;

/**
 * Base class for all response commands returned by the server or
 * client. Extends {@link Command} by attaching a status value that
 * indicates the outcome of processing the corresponding request.
 * <p>
 * Response commands are typically paired with a previously issued
 * request command using the same command ID. They communicate whether
 * the operation completed successfully or failed due to a specific
 * condition.
 * <p>
 * Subclasses may include additional payload describing the result of
 * the operation, such as error details or returned data.
 */
public abstract class CommandRe extends Command {

    /** Status of the processed command. Never {@code null}. */
    private final Command.Status status;

    /**
     * A predefined no-op response representing a successful empty
     * response. Can be used when no additional data must be returned.
     */
    public static final CommandRe NO_CMD = new CommandRe(Status.OK) {};

    /**
     * Enumeration of standard response statuses.
     * <p>
     * These values indicate generic outcomes of command processing.
     * Each constant includes a human-readable message that may be
     * logged or presented to the user.
     */
    public enum Status implements Command.Status {
        /** Operation completed successfully. */
        OK("OK"),

        /** An unexpected internal failure occurred during processing. */
        INTERNAL_ERROR("Internal error"),

        /** The addressed client could not be found on the server. */
        CLIENT_NOT_FOUND("Client not found"),
        ;

        /** A descriptive text associated with the status. */
        public final String msg;

        Status(String msg) {
            this.msg = msg;
        }
    }

    /**
     * Constructs a new response command with the given status.
     * <p>
     * If {@code null} is provided, status defaults to
     * {@link Status#INTERNAL_ERROR} to avoid carrying an undefined
     * response state.
     *
     * @param status status of the response
     */
    public CommandRe(Command.Status status) {
        this.status = Objects.requireNonNullElse(status,
                Status.INTERNAL_ERROR);
    }

    /**
     * Returns the status attached to this response command.
     *
     * @return the response status, never {@code null}
     */
    public Command.Status status() {
        return status;
    }
}
