package edu.io.net.command;

public class LeaveGame {

    public static class Cmd extends Command {
        public Cmd() {}

        @Override
        public String toString() {
            return "LeaveGameCmd{}";
        }
    }

    public static class CmdRe extends CommandRe {
        public enum Status implements Command.Status {
            OK("OK"),
            CLIENT_NOT_CONNECTED("Player with the given id is not connected!"),
            ;
            public final String msg;
            Status(String msg) {
                this.msg = msg;
            }
        }

        public CmdRe(Command.Status status) {
            super(status);
        }

        @Override
        public String toString() {
            return "LeaveGameCmdRe{" +
                    "status=" + status() +
                    '}';
        }
    }

}
