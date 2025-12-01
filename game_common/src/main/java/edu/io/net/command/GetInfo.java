package edu.io.net.command;

import java.io.Serializable;

public class GetInfo {

    public static class Cmd extends Command {
        public Cmd() {
        }

        @Override
        public String toString() {
            return "GetInfoCmd{}";
        }
    }

    public static class CmdRe extends CommandRe {
        public record Info(
                String clientId,
                Long gameStartedTimestamp,
                Long playerJoinedTimestamp
        ) implements Serializable {
            public static Info EMPTY = new Info("", 0L, 0L);
        }

        public final Info info;

        public CmdRe(Command.Status status, Info info) {
            super(status);
            this.info = info;
        }

        @Override
        public String toString() {
            return "GetInfoCmdRe{" + info.toString() + "}";
        }
    }
}
