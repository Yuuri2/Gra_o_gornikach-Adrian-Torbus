package edu.io.net;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import edu.io.net.command.Command;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the main entry point for game networking.
 * <p>
 * Manages communication between the game client and the remote server and
 * provides a high-level API to:
 * <ul>
 *   <li>establish and close connections,</li>
 *   <li>send commands,</li>
 *   <li>dispatch incoming commands and responses.</li>
 * </ul>
 * <p>
 * Two internal state indicators are maintained:
 * <ul>
 *   <li><b>State</b> — whether the connector is connected or
 *       disconnected,</li>
 *   <li><b>Status</b> — result of the last operation, consumed by
 *       {@link #onSuccess(Runnable)} and
 *       {@link #onFailure(Runnable)}.</li>
 * </ul>
 * <p>
 * Instances of this class are typically injected into game logic
 * components that require network interaction.
 * <p>
 * Supports sending commands with optional response handlers bound to
 * unique command IDs.
 * <p>
 * Thread safety: response dispatching to pending command handlers is
 * thread-safe, but callers must ensure thread-safe handling of any
 * shared state inside callbacks.
 */
public class GameServerConnector {
    private final URI uri;
    private final NetworkConnector netConnector;

    private final ConcurrentHashMap<Long, Consumer<Command>>
            pendingCommands = new ConcurrentHashMap<>();

    private State state;
    private Status status;
    private @NotNull Consumer<Command> cmdFromSrvHandler = cmd -> {};

    private enum State { DISCONNECTED, CONNECTED }

    private enum Status { NONE, OK, ERROR }

    /**
     * Creates a new {@code GameServerConnector} with a stub network
     * connector that does not perform real communication.
     * <p>
     * Expected connection string format:
     * {@code userid@host:port}
     *
     * @param connStr connection string identifying the remote server
     */
    public GameServerConnector(@NotNull String connStr) {
        this(connStr, new DumbNetworkConnector());
    }

    /**
     * Creates a new {@code GameServerConnector} with a custom network
     * connector.
     * <p>
     * Connection string must contain a host and a positive port.
     * User info is allowed but not interpreted.
     *
     * @param connString RFC 2396–compliant connection string,
     *                   e.g. {@code user@localhost:8080}
     * @param netConnector low-level network connector
     * @throws IllegalArgumentException if the connection string is
     *                                  invalid
     */
    public GameServerConnector(
            @NotNull String connString,
            @NotNull NetworkConnector netConnector) {
        uri = parseAndValidateConnString(connString);
        state = State.DISCONNECTED;
        status = Status.NONE;
        this.netConnector = netConnector;
    }

    /**
     * Checks whether the connector is currently connected.
     *
     * @return {@code true} if connected; {@code false} otherwise
     */
    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    /**
     * Attempts to establish a connection to the server.
     * <p>
     * Registers a handler dispatching incoming commands to their
     * matching pending responses or to the general handler.
     * <p>
     * On failure, no exception is thrown. State is set to
     * {@code DISCONNECTED}, status to {@code ERROR}.
     *
     * @return this connector for method chaining
     */
    public GameServerConnector connect() {
        try {
            netConnector.connect(uri);
            netConnector.onCmdFromServer(cmd -> {
                Objects.requireNonNullElse(
                        pendingCommands.remove(cmd.id()),
                        cmdFromSrvHandler
                ).accept(cmd);
            });
            state = State.CONNECTED;
            status = Status.OK;
        } catch (IOException e) {
            state = State.DISCONNECTED;
            status = Status.ERROR;
        }
        return this;
    }

    /**
     * Closes the connection to the server.
     * <p>
     * If an error occurs, state remains {@code DISCONNECTED} and
     * status becomes {@code ERROR}. No exception is thrown.
     *
     * @return this connector for method chaining
     */
    public GameServerConnector disconnect() {
        try {
            netConnector.disconnect();
            status = Status.OK;
        } catch (IOException e) {
            status = Status.ERROR;
        }
        state = State.DISCONNECTED;
        return this;
    }

    /**
     * Sends a command to the server without expecting a response.
     * <p>
     * On failure, wraps the underlying exception in a
     * {@code RuntimeException} and sets status to {@code ERROR}.
     *
     * @param cmd command to send
     * @return this connector for method chaining
     * @throws RuntimeException if sending fails
     */
    public GameServerConnector issueCommand(@NotNull Command cmd) {
        try {
            netConnector.sendToServer(cmd);
            status = Status.OK;
        } catch (Exception e) {
            status = Status.ERROR;
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Sends a command to the server and registers a callback for its
     * response.
     * <p>
     * The callback is registered only after the command has been
     * successfully sent. If sending fails, the callback is not stored
     * and the exception is propagated.
     *
     * @param cmd command to send
     * @param onResponse callback invoked when a response with the same
     *                   ID is received
     * @return this connector for method chaining
     * @throws RuntimeException if sending fails
     */
    public GameServerConnector issueCommand(
            @NotNull Command cmd,
            @NotNull Consumer<Command> onResponse) {
        try {
            issueCommand(cmd);
            pendingCommands.put(cmd.id(), onResponse);
            return this;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /**
     * Registers a general handler for all incoming commands that do
     * not match any pending command ID.
     * <p>
     * Replaces the previously registered handler.
     * <p>
     * If no handler is provided, unmatched commands are ignored.
     *
     * @param cmdHandler callback for incoming commands
     * @return this connector for method chaining
     */
    public GameServerConnector onCmdFromServer(
            @NotNull Consumer<Command> cmdHandler) {
        this.cmdFromSrvHandler = cmdHandler;
        return this;
    }

    /**
     * Executes the given action if the last operation succeeded.
     * <p>
     * After invocation, status is reset to {@code NONE}.
     *
     * @param action runnable executed on success
     * @return this connector for method chaining
     */
    public GameServerConnector onSuccess(@NotNull Runnable action) {
        if (status == Status.OK) {
            status = Status.NONE;
            action.run();
        }
        return this;
    }

    /**
     * Executes the given action if the last operation failed.
     * <p>
     * After invocation, status is reset to {@code NONE}.
     *
     * @param action runnable executed on failure
     * @return this connector for method chaining
     */
    public GameServerConnector onFailure(@NotNull Runnable action) {
        if (status == Status.ERROR) {
            status = Status.NONE;
            action.run();
        }
        return this;
    }

    // --- internal stub connector ---
    private static class DumbNetworkConnector
            implements NetworkConnector {
        @Override
        public void connect(URI uri) throws IOException {}

        @Override
        public void disconnect() throws IOException {}

        @Override
        public void sendToServer(Command cmd) throws IOException {}

        @Override
        public void onCmdFromServer(Consumer<Command> cmd) {}
    }

    /**
     * Parses and validates the connection string.
     * <p>
     * Requires a host and a positive port. User info is allowed but
     * ignored.
     *
     * @param connString connection string to parse
     * @return parsed URI
     * @throws IllegalArgumentException if invalid
     */
    private static URI parseAndValidateConnString(String connString) {
        try {
            var uri = URI.create(connString);
            if (uri.getHost() == null || uri.getPort() <= 0) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Invalid connection string", e);
        }
    }
}
