import edu.io.net.command.Command;
import edu.io.net.command.CommandAck;
import edu.io.net.command.CommandRe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandAckTest {

    private Command reqCmd;

    @BeforeEach
    void setUp() {
        reqCmd = new Command() {};
    }

    @Test
    void ack_id_matches_reqCmd_id() {
        var ack = new CommandAck(reqCmd);
        Assertions.assertEquals(ack.id(), reqCmd.id());
    }

    @Test
    void throws_when_cmd_is_null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> new CommandAck(null));
        Assertions.assertThrows(
                NullPointerException.class,
                () -> new CommandAck(reqCmd, null));
    }

    @Test
    void can_get_reqCmd() {
        Assertions.assertEquals(
                reqCmd,
                new CommandAck(reqCmd).reqCmd());
    }

    @Test
    void can_get_resCmd() {
        var resCmd = new CommandRe(CommandRe.Status.OK) {};
        Assertions.assertEquals(
                resCmd,
                new CommandAck(reqCmd, resCmd).resCmd());
    }

    @Test
    void empty_resCmd_is_not_null() {
        Assertions.assertNotNull(new CommandAck(reqCmd).resCmd());
    }

}
