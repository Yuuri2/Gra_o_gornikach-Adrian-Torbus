import edu.io.net.command.Command;
import edu.io.net.GameServerConnector;
import edu.io.net.SocketConnector;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SocketConnectorTest {
    private final int PORT = 1313;

    private volatile int testi;
    private volatile String testm;

    private ServerSocket serverSocket;
    private ObjectInputStream oIn;
    private ObjectOutputStream oOut;

    private GameServerConnector gsc;

    @BeforeEach
    void beforeEach() throws IOException {
        serverSocket = new ServerSocket(PORT);

        Thread.startVirtualThread(() -> {
            try (Socket sock = serverSocket.accept()) {
                oOut = new ObjectOutputStream(sock.getOutputStream());
                try (var in = new ObjectInputStream(sock.getInputStream())) {
                    while (sock.isConnected()) {
                        Object obj = in.readObject();
                        if (obj instanceof EchoCmd echoCmd) {
                            sendCmdToClient(echoCmd);
                        }
                        oOut.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        gsc = new GameServerConnector(
                "tcp://localhost:" + PORT,
                new SocketConnector());
    }

    // --- "server" methods
    private void sendCmdToClient(Command cmd) throws IOException {
        oOut.writeObject(cmd);
        oOut.flush();
    }

    @AfterEach
    void afterEach() throws IOException {
        serverSocket.close();
        gsc = null;
    }


    // --- tests
    @Test
    void can_connect_to_server() throws IOException, InterruptedException {
        var cnt = new CountDownLatch(1);
        Assertions.assertDoesNotThrow(() -> {
            gsc.connect();
            cnt.countDown();
        });
        cnt.await();
    }

    @Test
    void can_receive_cmd_from_server() throws IOException, InterruptedException {
        var cnt = new CountDownLatch(1);
        gsc.connect()
            .onCmdFromServer(cmd -> {
                Assertions.assertDoesNotThrow(() -> {
                    var msgCmd = (MsgCmd) cmd;
                    testm = msgCmd.msg();
                    cnt.countDown();
                });
            });
        sendCmdToClient(new MsgCmd("hello"));

        cnt.await(1, TimeUnit.SECONDS);
        Assertions.assertEquals("hello", testm);
    }

    @Test
    void echoes() throws InterruptedException {
        gsc.connect()
            .onCmdFromServer(cmd -> {
                Assertions.assertDoesNotThrow(() -> {
                    var echoCmd = (EchoCmd) cmd;
                    testm = echoCmd.msg();
                });
            });

        for (var s = "echo"; !s.isEmpty(); s = s.substring(0, s.length()-1)) {
            gsc.issueCommand(new EchoCmd(s));
            Thread.sleep(100);
            Assertions.assertEquals(s, testm);
            Thread.sleep(100);
        }
    }
}
