package edu.io.net.server;

import edu.io.net.Version;
import edu.io.net.command.*;

import java.io.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;

import edu.io.net.server.gameplay.Game;
import edu.io.net.server.gameplay.Player;
import edu.io.net.server.tcp.Client;
import edu.io.net.server.tcp.TCPServer;
import static edu.io.net.command.GameState.Pack.AFTER_JOIN_GAME;
import static edu.io.net.command.GameState.Pack.PLAYERS_LIST;

/**
 * High-level game server coordinating game logic and handling
 * game-related commands received via a lower-level {@link TCPServer}.
 *
 * <p>This class represents the domain layer of the server-side
 * architecture. It does <strong>not</strong> deal with network sockets
 * directly. Instead, it relies on an instance of {@link TCPServer}
 * which performs all TCP handling, connection acceptance and raw
 * command dispatching.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *     <li>maintaining and manipulating the {@link Game} instance
 *         representing the core game logic,</li>
 *     <li>registering handlers for all supported command types,</li>
 *     <li>processing commands in a per-client context,</li>
 *     <li>sending game updates or broadcast messages via the
 *         underlying {@code TCPServer},</li>
 *     <li>performing validation (e.g., protocol version checks),</li>
 *     <li>constructing and returning appropriate {@link CommandAck}
 *         responses.</li>
 * </ul>
 *
 * <p>All clients are represented by {@link Client} objects created
 * within {@code TCPServer}, which provides thread-safe I/O streams and
 * metadata such as connection timestamp and a per-client semaphore for
 * synchronous writes.
 *
 * <h2>Threading model</h2>
 * {@code TCPServer} creates one virtual thread per client and invokes
 * the registered command handler callback on that thread. All command
 * methods in this class run within those threads. Game logic methods
 * may therefore be invoked concurrently.
 *
 * <h2>Game state synchronization</h2>
 * Command handlers may issue broadcasts using
 * {@link TCPServer#broadcast(Command)} to synchronize updated game
 * state with all currently connected clients.
 */
public class GameServer {
    private final TCPServer server;
    private final Game game;

    private static Logger log() {
        return LoggerFactory.getLogger(GameServer.class);
    }

    /**
     * Entry point for running the game server as a standalone
     * application.
     *
     * <p>This method creates a new {@code GameServer} instance
     * listening on port {@code 1313} and starts it. The server will
     * initialize the game logic and begin accepting TCP client
     * connections.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
//        TODO: add cmd-line argument -p for port; default is 1313
        new GameServer(1313).start();
    }

    /**
     * Creates a new game server instance bound to the given TCP port.
     *
     * <p>This constructor initializes both major subsystems:
     * <ul>
     *     <li>a low-level {@link TCPServer} responsible for accepting
     *         client connections and dispatching incoming commands,</li>
     *     <li>a {@link Game} instance representing the core game logic
     *         and in-memory world state.</li>
     * </ul>
     *
     * <p>After constructing both components, the method registers all
     * supported command handlers by invoking
     * {@link #setupCommandRouter()}, effectively wiring the domain-level
     * logic to the network transport layer.
     *
     * @param port the TCP port on which the server will listen for
     *             incoming client connections
     */
    public GameServer(int port) {
        server = new TCPServer(port);
        game = new Game();
        setupCommandRouter();
    }

    /**
     * Starts the game server, including both the game logic loop and
     * the underlying TCP subsystem.
     *
     * <p>The method performs two high-level actions in order:
     * <ol>
     *     <li>invokes {@link Game#start()} to initialize or reset the
     *         internal game state and begin any game-specific background
     *         processes,</li>
     *     <li>starts the {@link TCPServer}, which begins accepting
     *         client connections and dispatching incoming commands to the
     *         handlers registered during construction.</li>
     * </ol>
     *
     * <p>If the TCP layer fails to start due to an {@link IOException},
     * the error is logged and the server remains inactive.
     *
     * <p><strong>Threading:</strong> Once started, the TCP server creates
     * a dedicated virtual thread for each connected client. All command
     * handlers defined in this class are executed on those threads.
     */
    public void start() {
        game.start();
        try {
            server.start();
        } catch (IOException e) {
            log().error("Failed to start server", e);
        }
    }

    /**
     * Registers command handlers for all supported game commands with
     * the underlying {@link TCPServer}.
     *
     * <p>This method wires domain-level game logic to the transport
     * layer. Each supported command type received from a client is
     * mapped to its corresponding handler method:
     * <ul>
     *     <li>{@link Echo.Cmd} → {@link #doEcho(Echo.Cmd)}</li>
     *     <li>{@link Handshake.Cmd} → {@link #doHandshake(Handshake.Cmd)}</li>
     *     <li>{@link JoinGame.Cmd} → {@link #doJoinGame(Client, JoinGame.Cmd)}</li>
     *     <li>{@link LeaveGame.Cmd} → {@link #doLeaveGame(Client, LeaveGame.Cmd)}</li>
     *     <li>{@link GetInfo.Cmd} → {@link #doGetInfo(Client, GetInfo.Cmd)}</li>
     * </ul>
     *
     * <p>Commands that do not match any known type are logged as errors
     * and result in {@link CommandAck#NO_ACK} being returned to the client.
     *
     * <p>This setup ensures that all incoming commands are processed
     * in the context of the appropriate client and allows the server to
     * maintain thread-safe and consistent game state across multiple
     * simultaneous connections.
     */
    private void setupCommandRouter() {
        server.onCommand((client, reqCmd) ->
            switch (reqCmd) {
                case Echo.Cmd cmd -> doEcho(cmd);
                case Handshake.Cmd cmd -> doHandshake(cmd);
                case JoinGame.Cmd cmd -> doJoinGame(client, cmd);
                case LeaveGame.Cmd cmd -> doLeaveGame(client, cmd);
                case GetInfo.Cmd cmd -> doGetInfo(client, cmd);
                default -> {
                    log().error("Unknown command: {}", reqCmd);
                    yield CommandAck.NO_ACK;
                }
            }
        );
    }

    /**
     * Handles an incoming handshake request from a newly connected client.
     *
     * <p>The handshake validates the connector library version supplied
     * by the client. The version string must match the format:
     * {@code MAJOR.MINOR[.PATCH]} (e.g. {@code 1.4} or {@code 2.0.3}).
     *
     * <p>The method checks whether the client's major/minor version is
     * compatible with the server protocol version defined in
     * {@link Version}. The following cases are handled:
     *
     * <ul>
     *   <li>malformed version string,</li>
     *   <li>client version too old,</li>
     *   <li>valid and compatible version.</li>
     * </ul>
     *
     * @param cmd handshake command sent by the client
     * @return acknowledgment containing the handshake result
     */
    private @NotNull CommandAck doHandshake(@NotNull Handshake.Cmd cmd) {
        var status = Handshake.CmdRe.Status.OK;
        var vs = Objects.requireNonNullElse(cmd.connectorVersion, "");
        Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.\\d+)?$");
        Matcher matcher = pattern.matcher(vs);
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            if (major < Version.MAJOR ||
                    (major == Version.MAJOR && minor < Version.MINOR)) {
                status = Handshake.CmdRe.Status.LIB_VERSION_TOO_LOW;
            }
        }
        else {
            status = Handshake.CmdRe.Status.LIB_VERSION_MALFORMED;
        }
        return new CommandAck(cmd, new Handshake.CmdRe(status));
    }

    /**
     * Handles an echo request by returning the same message back to the client.
     * Useful for debugging or checking connection health.
     *
     * @param cmd echo request command
     * @return acknowledgment containing the echoed string
     */
    private @NotNull CommandAck doEcho(@NotNull Echo.Cmd cmd) {
        return new CommandAck(cmd, new Echo.CmdRe(CommandRe.Status.OK, cmd.msg));
    }

    /**
     * Attempts to register the client as a participant in the running game.
     *
     * <p>A unique client identifier is generated based on the socket's
     * hash code. The identifier is stored on the {@link Client} object
     * upon successful registration.
     *
     * <p>On success, the server broadcasts an {@link UpdateState.Cmd}
     * to all connected clients to ensure that the global game state is
     * synchronized.
     *
     * @param client the client requesting to join
     * @param cmd join-game command
     * @return acknowledgment containing join status and assigned client ID
     */
    private @NotNull CommandAck doJoinGame(
            @NotNull Client client,
            @NotNull JoinGame.Cmd cmd) {
        var clientId = "%x".formatted(client.socket().hashCode());
        var status = game.addPlayer(clientId, new Player(cmd));
        if (status == JoinGame.CmdRe.Status.OK) {
            client.setId(clientId);
            server.sendToClient(client, UpdateStateFactory.create(AFTER_JOIN_GAME));
            server.broadcast(UpdateStateFactory.create(PLAYERS_LIST));
        }
        return new CommandAck(cmd, new JoinGame.CmdRe(status, clientId));
    }

    /**
     * Removes the client from the game session.
     *
     * <p>If the client is registered, their player entry is removed
     * from the {@link Game} instance. If the client was not known to
     * the server, the result indicates {@code CLIENT_NOT_CONNECTED}.
     *
     * @param client the leaving client
     * @param cmd leave-game command
     * @return acknowledgment containing leave status
     */
    private @NotNull CommandAck doLeaveGame(
            @NotNull Client client,
            @NotNull LeaveGame.Cmd cmd) {
        Command.Status status = LeaveGame.CmdRe.Status.CLIENT_NOT_CONNECTED;
        if (server.getClientBySocket(client.socket()).isPresent()) {
            status = game.removePlayer(client.id());
            if (status == LeaveGame.CmdRe.Status.OK) {
                server.broadcast(UpdateStateFactory.create(PLAYERS_LIST));
            }
        }
        return new CommandAck(cmd, new LeaveGame.CmdRe(status));
    }

    /**
     * Returns basic information about the requesting client as well as
     * relevant global game status.
     *
     * <p>If the client is not yet registered (has no assigned ID),
     * the response contains {@code CLIENT_NOT_FOUND}.
     *
     * <p>On success, the returned info includes:
     * <ul>
     *   <li>client identifier,</li>
     *   <li>the timestamp of initial connection,</li>
     *   <li>the timestamp when the game session started.</li>
     * </ul>
     *
     * @param client requesting client
     * @param cmd get-info command
     * @return acknowledgment wrapping a {@link GetInfo.CmdRe.Info} object
     */
    private @NotNull CommandAck doGetInfo(
            @NotNull Client client,
            @NotNull GetInfo.Cmd cmd) {
        Command.Status status;
        GetInfo.CmdRe.Info info;
        if (client.id().isBlank()) {
            status = CommandRe.Status.CLIENT_NOT_FOUND;
            info = GetInfo.CmdRe.Info.EMPTY;
        }
        else {
            status = CommandRe.Status.OK;
            info = new GetInfo.CmdRe.Info(
                    client.id(),
                    game.gameStartedTimestamp,
                    client.timestamp()
            );
        }
        return new CommandAck(cmd,
                new GetInfo.CmdRe(status, info));
    }

}
