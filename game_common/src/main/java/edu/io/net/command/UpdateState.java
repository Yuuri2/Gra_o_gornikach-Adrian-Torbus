package edu.io.net.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a command used to transmit updated game state
 * information from the server to clients.
 *
 * <p>An {@code UpdateState} command carries a collection of
 * {@link GameState} objects, each describing a specific aspect
 * of the game (e.g., board squares, player information, or
 * player lists). Update commands are usually broadcast to
 * all connected clients whenever the game state changes.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * UpdateState.Cmd cmd = new UpdateState.Cmd(UpdateState.Pack.AFTER_JOIN_GAME);
 * cmd.add(new GameState.BoardInfo(8));
 * server.broadcast(cmd);
 * }</pre>
 */
public class UpdateState {
    /**
     * Command sent from the server to clients containing updated
     * game state information.
     */
    public static class Cmd extends Command {

        /** List of state objects included in this update. */
        public final List<GameState> stateInfoList = new ArrayList<>();

        /** Specifies the type of update being sent. */
        private final transient GameState.Pack pack;

        /**
         * Creates a new update command for the specified pack.
         *
         * @param pack type of update
         */
        public Cmd(GameState.Pack pack) {
            this.pack = pack;
        }

        /**
         * Returns the type of update contained in this command.
         *
         * @return the pack type
         */
        public GameState.Pack pack() {
            return pack;
        }

        /**
         * Adds a {@link GameState} object to this update command.
         *
         * @param stateInfo game state object to include
         * @return this {@code Cmd} instance for method chaining
         * @throws NullPointerException if {@code stateInfo} is null
         */
        public UpdateState.Cmd add(@NotNull GameState stateInfo) {
            Objects.requireNonNull(stateInfo, "stateInfo can't be null");
            stateInfoList.add(stateInfo);
            return this;
        }

        @Override
        public String toString() {
            return "UpdateStateCmd{%s}"
                    .formatted(stateInfoList.toString());
        }
    }

    /**
     * Response command acknowledging the receipt and processing
     * of an {@link UpdateState.Cmd}.
     */
    public static class CmdRe extends CommandRe {

        /**
         * Status of the update state operation.
         */
        public enum Status implements Command.Status {
            /** Update applied successfully. */
            OK("Updated");

            /** Human-readable status message. */
            public final String msg;

            Status(String msg) {
                this.msg = msg;
            }
        }

        /**
         * Constructs a response command with the given status.
         *
         * @param status result of the update state operation
         */
        public CmdRe(Command.Status status) {
            super(status);
        }

        @Override
        public String toString() {
            return "UpdateStateCmdRe{status='%s'}"
                    .formatted(status());
        }
    }
}
