package edu.io.net;

import edu.io.net.command.Command;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

/**
 * Defines a low-level abstraction for network communication between
 * the game client and the server.
 * <p>
 * Implementations of this interface are responsible for managing the
 * connection lifecycle, sending commands, and delivering messages
 * received from the server to registered listeners.
 */
public interface NetworkConnector {

    /**
     * Establishes a connection to the specified remote endpoint.
     *
     * @param uri the URI of the remote server to connect to
     * @throws IOException if the connection cannot be established due
     *                     to a network or I/O error
     */
    void connect(URI uri) throws IOException;

    /**
     * Terminates the current connection to the remote server.
     *
     * @throws IOException if an error occurs while closing the
     *                     connection
     */
    void disconnect() throws IOException;

    /**
     * Sends a command object to the connected remote server.
     *
     * @param cmd the {@link Command} to be transmitted
     * @throws IOException if sending fails due to a network or I/O
     *                     problem
     */
    void sendToServer(Command cmd) throws IOException;

    /**
     * Registers a callback that will be invoked whenever a command is
     * received from the server.
     *
     * @param cmd consumer that processes incoming {@link Command}
     *             objects
     */
    void onCmdFromServer(Consumer<Command> cmd);
}
