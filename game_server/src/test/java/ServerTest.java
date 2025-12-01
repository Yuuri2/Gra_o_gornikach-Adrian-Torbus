import edu.io.net.GameServerConnector;
import edu.io.net.SocketConnector;
import edu.io.net.Version;
import edu.io.net.command.*;
import edu.io.net.server.GameServer;
import edu.io.net.server.tcp.TCPServer;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class ServerTest {
    private static Thread thread;

    private static final int PORT = 1313;
    private static final String connStr = "tcp://localhost:" + PORT;
    private static GameServer gs;

    private GameServerConnector gsc;
    private AtomicReference<Command> resp;
    private AtomicReference<Command> cmdFromSrv;
    private AtomicReference<String> testm;

    @BeforeAll
    static void beforeAll() {
        thread = Thread.startVirtualThread(() -> {
            gs = new GameServer(PORT);
            gs.start();
        });
    }

    @AfterAll
    static void afterAll() {
        thread.interrupt();
    }

    @BeforeEach
    void beforeEach() {
        gsc = new GameServerConnector(connStr, new SocketConnector());
        gsc.connect();
        resp = new AtomicReference<>();
        cmdFromSrv = new AtomicReference<>();
        testm = new AtomicReference<>();
    }

    private void issueCommandAndThen(Command cmd, Consumer<CommandAck> code) {
        gsc.issueCommand(cmd, resp::set);
        await().atMost(1, SECONDS)
                .untilAsserted(() -> {
                    if  ((resp.get() instanceof CommandAck res)) {
                        code.accept(res);
                    }
                });
    }

    @Test
    void can_connect_to_server() {
        Assertions.assertTrue(gsc.isConnected());
    }

    @Test
    void echo_responds() {
        var cmd = new Echo.Cmd("hello");
        gsc.issueCommand(cmd, resp::set);
        await().atMost(1, SECONDS)
            .untilAsserted(() -> {
                if ((resp.get() instanceof CommandAck res)
                    && (res.resCmd() instanceof Echo.CmdRe echoCmdRe)) {
                    Assertions.assertEquals(cmd.msg, echoCmdRe.msg);
                }
                else Assertions.fail();
            });
    }

    @Test
    void handshake_successful() {
        var cmd = new Handshake.Cmd(
                "%d.%d".formatted(Version.MAJOR, Version.MINOR));
        issueCommandAndThen(cmd, (ack) -> {
            Assertions.assertEquals(
                    Handshake.CmdRe.Status.OK,
                    ack.resCmd().status()
            );
        });
    }

    @Test
    void handshake_failed_when_major_version_is_too_low() {
        var cmd = new Handshake.Cmd("0.1");
        issueCommandAndThen(cmd, (ack) -> {
            Assertions.assertEquals(
                    Handshake.CmdRe.Status.LIB_VERSION_TOO_LOW,
                    ack.resCmd().status()
            );
        });
    }

    @Test
    void handshake_failed_when_minor_version_is_too_low() {
        var cmd = new Handshake.Cmd("1.0");
        issueCommandAndThen(cmd, (ack) -> {
            Assertions.assertEquals(
                    Handshake.CmdRe.Status.LIB_VERSION_TOO_LOW,
                    ack.resCmd().status()
            );
        });
    }

    @Test
    void handshake_failed_when_version_is_malformed() {
        var cmd = new Handshake.Cmd("3");
        issueCommandAndThen(cmd, (ack) -> {
            Assertions.assertEquals(
                    Handshake.CmdRe.Status.LIB_VERSION_MALFORMED,
                    ack.resCmd().status()
            );
        });
    }

    @Test
    void join_player_successful() {
        var cmd = new JoinGame.Cmd("ziutek");
        issueCommandAndThen(cmd, (ack) -> {
            Assertions.assertEquals(
                    JoinGame.CmdRe.Status.OK,
                    ack.resCmd().status()
            );
        });
    }

    @Test
    void cannot_join_twice() {
        var cmd = new JoinGame.Cmd("ziutek");
        issueCommandAndThen(cmd, (ack) -> {});
        issueCommandAndThen(cmd, (ack) -> {
            Assertions.assertEquals(
                    JoinGame.CmdRe.Status.ALREADY_CONNECTED,
                    ack.resCmd().status()
            );
        });
    }

    @Test
    void after_joining_player_get_clientId() {
        var cmd = new JoinGame.Cmd("ziutek");
        issueCommandAndThen(cmd, (ack) -> {
            if (ack.resCmd() instanceof JoinGame.CmdRe joinGameCmdRe) {
                Assertions.assertNotNull(joinGameCmdRe.clientId);
                Assertions.assertFalse(joinGameCmdRe.clientId.isBlank());
            }
        });
    }

    @Test
    void player_can_leave_game() {
        var joinCmd = new JoinGame.Cmd("ziutek");
        issueCommandAndThen(joinCmd, (joinAck) -> {
            var leaveCmd = new LeaveGame.Cmd();
            issueCommandAndThen(leaveCmd, leaveRe -> {
                Assertions.assertEquals(
                        LeaveGame.CmdRe.Status.OK,
                        leaveRe.resCmd().status()
                );
            });
        });
    }

    @Test
    void cannot_leave_game_if_not_connected() {
        issueCommandAndThen(new JoinGame.Cmd("ziutek"), joinAck -> {
            var leaveCmd = new LeaveGame.Cmd();
            issueCommandAndThen(leaveCmd, leave1Ack -> {
                issueCommandAndThen(leaveCmd, leave2Ack -> {
                    Assertions.assertEquals(
                            LeaveGame.CmdRe.Status.CLIENT_NOT_CONNECTED,
                            leave2Ack.resCmd().status());
                });
            });
        });
    }

    @Test
    void can_get_info_after_join() {
        issueCommandAndThen(new JoinGame.Cmd("ziutek"), joinAck -> {
            issueCommandAndThen(new GetInfo.Cmd(), ack -> {
                Assertions.assertEquals(
                        CommandRe.Status.OK,
                        ack.resCmd().status()
                );
            });
        });
    }

    @Test
    void cannot_get_info_if_not_joined() {
        issueCommandAndThen(new GetInfo.Cmd(), ack -> {
            Assertions.assertEquals(
                    CommandRe.Status.CLIENT_NOT_FOUND,
                    ack.resCmd().status()
            );
            Assertions.assertEquals(
                    GetInfo.CmdRe.Info.EMPTY,
                    ((GetInfo.CmdRe)ack.resCmd()).info
            );
        });
    }

    @Test
    void srv_can_send_cmd() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        testm.set("");
        var cnt = new CountDownLatch(1);
        gsc.onCmdFromServer(cmd -> {
            if (cmd instanceof Echo.Cmd echoCmd) {
                testm.set(echoCmd.msg);
                cnt.countDown();
            }
        });

        // ugh... dirty hack with reflection (only for test purposes)
        var serverField = GameServer.class.getDeclaredField("server");
        serverField.setAccessible(true);
        ((TCPServer)serverField.get(gs)).broadcast(new Echo.Cmd("hello!"));
        cnt.await(1, TimeUnit.SECONDS);
        Assertions.assertEquals("hello!", testm.get());
    }

    @Test
    void after_JoinGame_server_send_UpdateState() {
        gsc.onCmdFromServer(cmdFromSrv::set);
        gsc.issueCommand(new JoinGame.Cmd("ziutek"));
        await().atMost(1, SECONDS)
                .untilAsserted(() -> {
                    if (cmdFromSrv.get() instanceof UpdateState.Cmd update) {
                        Assertions.assertTrue(true);
                    }
                    else Assertions.fail();
                });
    }
}
