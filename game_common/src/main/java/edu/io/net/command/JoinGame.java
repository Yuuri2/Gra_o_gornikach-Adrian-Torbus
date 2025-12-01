package edu.io.net.command;

public class JoinGame {

    public static class Cmd extends Command {
        public final String name;
        public Cmd(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "JoinGameCmd{name='%s'}".formatted(name);
        }
    }


    public static class CmdRe extends CommandRe {
        public final String clientId;

        public enum Status implements Command.Status {
            OK("Hello! Nice to meet you!"),
            NAME_ALREADY_EXISTS("Name already exists!"),
            ALREADY_CONNECTED("Player already connected!"),
            ;
            public final String msg;
            Status(String msg) {
                this.msg = msg;
            }
        }

        public CmdRe(Command.Status status, String clientId) {
            super(status);
            this.clientId = clientId;
        }

        @Override
        public String toString() {
            return "JoinGameCmdRe{status='%s', clientId='%s'}"
                    .formatted(status(), clientId);
        }
    }

}
