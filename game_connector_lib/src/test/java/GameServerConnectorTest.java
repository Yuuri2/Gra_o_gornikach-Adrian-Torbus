import edu.io.net.command.Command;
import edu.io.net.GameServerConnector;
import edu.io.net.NetworkConnector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GameServerConnectorTest {

    private final String CONN_STR_MIN = "tcp://localhost:1313";
    private final String CONN_STR_WITH_UID = "tcp://321364@localhost:1313";
    private final String TEST_MSG = "message";

    private GameServerConnector gsc_dumb;
    private GameServerConnector gsc_spy;
    private GameServerConnector gsc_fail;

    private NetworkConnector nc_fail;
    private NetworkConnector nc_spy;
    private NetworkConnector nc_bcast;
    private NetworkConnector nc_echo;

    private String nc_spy_scheme;
    private String nc_spy_host;
    private int nc_spy_port;
    private String nc_spy_userInfo;
    private String nc_spy_msg;
    private boolean test;
    private int testi;
    private String testm;
    private long testl;

    @BeforeEach
    void setUp() {
        nc_spy_scheme = "";
        nc_spy_host = "";
        nc_spy_port = 0;
        nc_spy_userInfo = "";
        nc_spy_msg = "";
        test = false;
        testi = 0;
        testm = "";

        nc_spy = new NetworkConnector() {
            @Override
            public void connect(URI uri) throws IOException {
                nc_spy_scheme = uri.getScheme();
                nc_spy_host = uri.getHost();
                nc_spy_port = uri.getPort();
                nc_spy_userInfo = uri.getUserInfo();
                nc_spy_msg = "connect";
            }
            @Override
            public void disconnect() throws IOException {
                nc_spy_msg = "disconnect";
            }
            @Override
            public void sendToServer(Command cmd) throws IOException {
                nc_spy_msg = "send";
            }
            @Override
            public void onCmdFromServer(Consumer<Command> cmd) {
                nc_spy_msg = "onMsgFromServer";
            }
        };

        nc_fail = new NetworkConnector() {
            @Override
            public void connect(URI uri) throws IOException {
                throw new IOException();
            }
            @Override
            public void disconnect() throws IOException {
                throw new IOException();
            }
            @Override
            public void sendToServer(Command cmd) throws IOException {
                throw new IOException();
            }
            @Override
            public void onCmdFromServer(Consumer<Command> cmd) {
                throw new RuntimeException();
            }
        };

        nc_bcast = new NetworkConnector() {
            @Override
            public void connect(URI uri) throws IOException {}
            @Override
            public void disconnect() throws IOException {}
            @Override
            public void sendToServer(Command cmd) throws IOException {}
            @Override
            public void onCmdFromServer(Consumer<Command> cmd) {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    cmd.accept(new MsgCmd(TEST_MSG));
                }).start();
            }
        };

        nc_echo = new NetworkConnector() {
            private String msg = "";
            private Command command;
            @Override
            public void connect(URI uri) throws IOException {}
            @Override
            public void disconnect() throws IOException {}
            @Override
            public void sendToServer(Command cmd) throws IOException {
                if (cmd instanceof EchoCmd echoCmd) {
                    command = echoCmd;
                    msg = echoCmd.msg();
                }
            }
            @Override
            public void onCmdFromServer(Consumer<Command> cmd) {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    cmd.accept(new EchoAck(command.id(), msg));
                }).start();
            }
        };

        gsc_dumb = new GameServerConnector(CONN_STR_MIN);
        gsc_spy = new GameServerConnector(CONN_STR_WITH_UID, nc_spy);
        gsc_fail = new GameServerConnector(CONN_STR_MIN, nc_fail);
    }

    @Test
    void connString_must_include_addr_and_port() {
        Assertions.assertDoesNotThrow(() -> {
            new GameServerConnector(CONN_STR_MIN);
        });
        var strs = new String[] {
                null, "", "localhost", ":", "localhost:", "155,555?:1313",
                ":1313", "localhost:1313"
        };
        for (var str : strs) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                new GameServerConnector(str);
            });
        }
    }

    @Test
    void after_creation_status_should_be_DISCONNECTED() {
        Assertions.assertFalse(gsc_dumb.isConnected());
    }

    @Test
    void GameServerConnector_interface_is_fluent() {
        var rets = new GameServerConnector[] {
                gsc_dumb.connect(),
                gsc_dumb.disconnect(),
                gsc_dumb.issueCommand(new Command() {}),
        };
        for (var ret : rets) {
            Assertions.assertEquals(gsc_dumb, ret);
        }
    }

    @Test
    void can_connect() {
        Assertions.assertDoesNotThrow(() ->
                gsc_dumb.connect());
        Assertions.assertTrue(gsc_dumb.isConnected());

        test = false;
        gsc_dumb.connect().onSuccess(() -> test = true);
        Assertions.assertTrue(test);
    }

    @Test
    void if_gsc_pass_conn_params_to_network_connector() {
        Assertions.assertDoesNotThrow(() ->
                gsc_spy.connect());
        Assertions.assertEquals("localhost", nc_spy_host);
        Assertions.assertEquals(1313, nc_spy_port);
        Assertions.assertEquals("321364", nc_spy_userInfo);
        Assertions.assertEquals("onMsgFromServer", nc_spy_msg);
    }

    @Test
    void connect_can_fail() {
        Assertions.assertDoesNotThrow(() -> {
            testi = 0;
            gsc_fail.connect()
                    .onSuccess(() -> testi = 1)
                    .onFailure(() -> testi = 2);
            Assertions.assertEquals(2, testi);
        });
        Assertions.assertFalse(gsc_fail.isConnected());
    }

    @Test
    void onSuccess_and_onFailure_is_oneshot_op() {
        gsc_dumb.connect();
        testi = 0;
        gsc_dumb.onSuccess(() -> testi = 1 );
        gsc_dumb.onSuccess(() -> testi = 2 );
        Assertions.assertEquals(1, testi);

        gsc_fail.connect();
        testi = 0;
        gsc_fail.onFailure(() -> testi = 1 );
        gsc_fail.onFailure(() -> testi = 2 );
        Assertions.assertEquals(1, testi);
    }

    @Test
    void can_disconnect() {
        Assertions.assertDoesNotThrow(() -> {
            gsc_spy.connect();
            gsc_spy.disconnect();
        });
        Assertions.assertFalse(gsc_spy.isConnected());
        Assertions.assertEquals("disconnect", nc_spy_msg);
    }

    @Test
    void can_issue_command() {
        test = false;
        Assertions.assertDoesNotThrow(() -> {
            gsc_spy
                .connect()
                .issueCommand(new Command() {});
        });
        Assertions.assertEquals("send", nc_spy_msg);
        test = false;
        gsc_spy.onSuccess(() -> test = true);
        Assertions.assertTrue(test);
    }

    @Test
    void can_handle_bcasts_from_server() throws InterruptedException {
        testm = "";
        var cnt = new CountDownLatch(1);
        new GameServerConnector(CONN_STR_MIN,  nc_bcast)
            .connect()
            .onCmdFromServer(cmd -> {
                if (cmd instanceof MsgCmd msgCmd) {
                    testm = msgCmd.msg();
                    cnt.countDown();
                }
            });
        cnt.await(1, TimeUnit.SECONDS);
        Assertions.assertEquals(TEST_MSG, testm);
    }

    @Test
    void echo_responds() throws InterruptedException {
        var cnt = new CountDownLatch(1);
        new GameServerConnector(CONN_STR_MIN, nc_echo)
            .connect()
            .onCmdFromServer(cmd -> {
                if (cmd instanceof EchoAck msgCmd) {
                    Assertions.assertEquals("hello", msgCmd.msg());
                    cnt.countDown();
                }
            })
            .issueCommand(new EchoCmd("hello"))
            .onFailure(Assertions::fail);
        cnt.await(1, TimeUnit.SECONDS);
    }

    @Test
    void server_respond_with_same_id() throws InterruptedException {
        var cnt = new CountDownLatch(1);
        var cmd =  new EchoCmd("hello");
        testl = cmd.id();
        new GameServerConnector(CONN_STR_MIN, nc_echo)
                .connect()
                .onCmdFromServer(res -> {
                    if (res instanceof EchoAck echoCmdAck) {
                        Assertions.assertEquals(testl, echoCmdAck.id());
                        cnt.countDown();
                    }
                })
                .issueCommand(cmd)
                .onFailure(Assertions::fail);
        cnt.await();
    }

    @Test
    void echo_responds_using_handler() throws InterruptedException {
        var cnt = new CountDownLatch(1);
        var cmd =  new EchoCmd("hello");
        new GameServerConnector(CONN_STR_MIN, nc_echo)
                .connect()
                .issueCommand(cmd, res -> {
                    if (res instanceof EchoAck echoCmdAck) {
                        testm = echoCmdAck.msg();
                        testl = echoCmdAck.id();
                        cnt.countDown();
                    }
                })
                .onFailure(Assertions::fail);
        cnt.await(1, TimeUnit.SECONDS);
        Assertions.assertEquals("hello", testm);
        Assertions.assertEquals(cmd.id(), testl);
    }

}
