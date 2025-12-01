package edu.io.net.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Factory class for creating {@link UpdateState.Cmd} instances.
 *
 * <p>This class manages a list of {@link StateSource} providers
 * which can populate update commands with additional game state
 * information. It centralizes the construction of {@code UpdateState.Cmd}
 * objects and ensures that all registered sources contribute
 * to each generated update.
 */
public class UpdateStateFactory {

    /** Registered state sources that populate update commands. */
    private static final List<StateSource> sources = new ArrayList<>();

    /**
     * Represents a source of state updates for an {@link UpdateState.Cmd}.
     * <p>
     * Implementations of this interface add game state data to the
     * update command when {@link #populate(UpdateState.Cmd)} is invoked.
     */
    public interface StateSource {
        /**
         * Populates the given update command with state data.
         *
         * @param cmd the update command to populate
         */
        void populate(UpdateState.Cmd cmd);
    }

    /**
     * Registers a new state source that will be used when creating
     * {@link UpdateState.Cmd} instances.
     *
     * @param source the state source to register
     * @throws NullPointerException if {@code source} is null
     */
    public static void register(@NotNull StateSource source) {
        sources.add(Objects.requireNonNull(source));
    }

    /**
     * Creates a new {@link UpdateState.Cmd} populated by all
     * registered {@link StateSource} instances.
     *
     * @param pack the {@link GameState.Pack} to initialize the command
     * @return a new {@code UpdateState.Cmd} populated with data from all
     *         registered state sources
     */
    public static UpdateState.Cmd create(@NotNull GameState.Pack pack) {
        var cmd = new UpdateState.Cmd(pack);
        for (var source : sources) {
            source.populate(cmd);
        }
        return cmd;
    }
}
