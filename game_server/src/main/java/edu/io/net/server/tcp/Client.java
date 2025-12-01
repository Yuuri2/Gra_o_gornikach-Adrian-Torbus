package edu.io.net.server.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * Represents a single connected game client in the TCP-based server
 * architecture.
 * <p>
 * A {@code Client} instance wraps a TCP socket together with the
 * associated I/O streams and metadata required to manage client
 * communication. It provides access to the client's identifier,
 * connection timestamp and a semaphore used to coordinate exclusive
 * access to the output stream.
 * <p>
 * The server creates one {@code Client} object per accepted socket.
 * This class does not spawn its own threads; it is typically used by
 * higher-level server components responsible for message handling and
 * request routing.
 */
public class Client {

    /** Identifier of the client, typically set after authentication. */
    private String id;

    /** Timestamp of the moment the client connected to the server. */
    private final long timestamp;

    /**
     * Semaphore guarding write access to the output stream.
     * <p>
     * Since {@link ObjectOutputStream} is not thread-safe, server
     * handlers should acquire this semaphore before sending data to
     * the client to ensure serialized, ordered writes.
     */
    private final Semaphore semaphore;

    /** Underlying TCP socket used for communication. */
    private final Socket socket;

    /** Output stream for sending serialized objects to the client. */
    private final ObjectOutputStream oOut;

    /** Input stream for receiving serialized objects from the client. */
    private final ObjectInputStream oIn;

    /**
     * Creates a new {@code Client} with the given socket, identifier
     * and connection timestamp.
     *
     * @param socket underlying TCP socket
     * @param id initial identifier of the client
     * @param timestamp time of connection, in milliseconds
     * @throws IOException if stream initialization fails
     */
    public Client(Socket socket, String id, long timestamp)
            throws IOException {
        this.id = id;
        this.timestamp = timestamp;
        this.semaphore = new Semaphore(1);
        this.socket = socket;
        this.oOut = new ObjectOutputStream(socket.getOutputStream());
        this.oIn = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Creates a new {@code Client} with a provided identifier and
     * current system time as the connection timestamp.
     *
     * @param socket TCP socket associated with the client
     * @param clientId identifier of the client
     * @throws IOException if stream initialization fails
     */
    public Client(Socket socket, String clientId) throws IOException {
        this(socket, clientId, System.currentTimeMillis());
    }

    /**
     * Creates a new {@code Client} without an explicit identifier.
     * <p>
     * The identifier may later be set using
     * {@link #setId(String)}.
     *
     * @param socket TCP socket associated with the client
     * @throws IOException if stream initialization fails
     */
    public Client(Socket socket) throws IOException {
        this(socket, "");
    }

    /** Returns the client's identifier. */
    public String id() {
        return id;
    }

    /** Sets the client's identifier. */
    public void setId(String clientId) {
        this.id = clientId;
    }

    /** Returns the connection timestamp in milliseconds. */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns the semaphore used to synchronize write operations.
     * <p>
     * Handlers should acquire this semaphore before calling
     * {@link #out()}.
     */
    public Semaphore semaphore() {
        return semaphore;
    }

    /** Returns the underlying TCP socket. */
    public Socket socket() {
        return socket;
    }

    /** Returns the output stream for sending objects to the client. */
    public ObjectOutputStream out() {
        return oOut;
    }

    /** Returns the input stream for receiving objects from the client. */
    public ObjectInputStream in() {
        return oIn;
    }
}

