package edu.io.net.command;

import edu.io.net.Version;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Handshake {

    public static class Cmd extends Command {
        public final String connectorVersion;
        public final Map<String, String> metadata;
        public Cmd(@NotNull String connectorVersion) {
            this(connectorVersion, null);
        }
        public Cmd(
                @NotNull String connectorVersion,
                @NotNull Map<String, String> metadata) {
            this.connectorVersion = Objects.requireNonNull(connectorVersion);
            this.metadata = Objects.requireNonNullElse(metadata, new HashMap<>());
        }

        @Override
        public String toString() {
            return "HandshakeCmd{connectorVersion=%s, metadata=%s}"
                    .formatted(connectorVersion, metadata);
        }
    }

    public static class CmdRe extends CommandRe {
        public enum Status implements Command.Status {
            OK("Welcome to the game!"),
            LIB_VERSION_TOO_LOW("Upgrade library to version %d.%d.x"
                    .formatted(Version.MAJOR, Version.MINOR)),
            LIB_VERSION_MALFORMED("Malformed library version. " +
                    "Expected format: MAJOR.MINOR[.BUILD], e.g. 1.2 or 1.2.13"),
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
            return "HandshakeReCmd{status=%s}".formatted(status());
        }
    }
}
