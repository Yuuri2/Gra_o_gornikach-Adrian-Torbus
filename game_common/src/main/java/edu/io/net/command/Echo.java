package edu.io.net.command;

public class Echo {

    public static class Cmd extends Command {
        public final String msg;

        public Cmd(String msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return "EchoCmd{msg='%s'}".formatted(msg);
        }
    }

    public static class CmdRe extends CommandRe {
        public final String msg;

        public CmdRe(Command.Status status, String msg) {
            super(status);
            this.msg = msg;
        }

        @Override
        public String toString() {
            return "EchoCmdRe{" +
                    "msg='" + msg + '\'' +
                    '}';
        }
    }

}
