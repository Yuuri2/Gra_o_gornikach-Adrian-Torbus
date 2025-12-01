import edu.io.net.command.Command;

class MsgCmd extends Command {
    private final String msg;
    public MsgCmd(String msg) {
        this.msg = msg;
    }
    public String msg()  {
        return msg;
    }
}
class EchoCmd extends Command {
    private final String msg;
    public EchoCmd(String msg) {
        this.msg = msg;
    }
    public String msg()  {
        return msg;
    }
}
class EchoAck extends Command {
    private final String msg;
    public EchoAck(long cmdId, String msg) {
        super(cmdId);
        this.msg = msg;
    }
    public String msg()  {
        return msg;
    }
}
