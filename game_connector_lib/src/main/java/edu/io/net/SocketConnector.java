package edu.io.net;

import edu.io.net.command.Command;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.util.function.Consumer;

/**
 * Default implementation of {@link NetworkConnector} that uses TCP
 * sockets for communication between the client and the game server.
 * <p>
 * This class manages the socket connection, handles input and output
 * streams, and delivers received {@link Command} objects to a
 * registered listener.
 * <p>
 * The connector is thread-safe for concurrent send and receive
 * operations, but the connection lifecycle methods
 * ({@code connect()} and {@code disconnect()}) should not be called
 * concurrently.
 */
public class SocketConnector implements NetworkConnector {
    private Socket sock;
    private ObjectInputStream oIn;
    private ObjectOutputStream oOut;

    @Override
    public void connect(URI uri) throws IOException {
        sock = new Socket(uri.getHost(), uri.getPort());
        oOut = new ObjectOutputStream(sock.getOutputStream());
        oIn = new ObjectInputStream(sock.getInputStream());
    }

    @Override
    public void disconnect() throws IOException {
        sock.close();
    }

    @Override
    public void sendToServer(Command cmd) throws IOException {
        oOut.writeObject(cmd);
        oOut.flush();
    }

    @Override
    public void onCmdFromServer(Consumer<Command> cmd) {
        Thread.startVirtualThread(() -> {
            try {
                while (sock.isConnected()) {
                    cmd.accept((Command) oIn.readObject());
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
