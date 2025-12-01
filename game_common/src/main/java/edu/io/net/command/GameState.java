package edu.io.net.command;

import java.io.Serializable;
import java.util.List;

/**
 * Marker interface for all serializable data structures representing
 * the state of various elements in the game.
 * <p>
 * Implementations of this interface are immutable data carriers
 * designed to be transmitted between the server and connected clients
 * as part of the game's state synchronization mechanism.
 * <p>
 * The nested record types provide structured representations of:
 * <ul>
 *     <li>the game board and its metadata,</li>
 *     <li>individual board squares,</li>
 *     <li>the list of players and the information about the currently active one,</li>
 *     <li>the attributes of a single player,</li>
 *     <li>utility structures describing positions and lightweight player descriptors.</li>
 * </ul>
 */
public interface GameState extends Serializable {
    /**
     * Specifies which part of the game state is being updated.
     */
    enum Pack {
        /** Update triggered after a player joins the game. */
        AFTER_JOIN_GAME,
        /** Update containing information about a board square. */
        BOARD_SQUARE,
        /** Update containing a list of players and their statuses. */
        PLAYERS_LIST,
        /** Update containing a single player's detailed information. */
        PLAYER,
    }

    /**
     * Immutable descriptor of global board metadata.
     *
     * @param size the width/height of the board (assumed square)
     */
    record BoardInfo(int size) implements GameState {}

    /**
     * Information about a single square on the game board.
     *
     * @param pos   the board position of this square
     * @param label a logical label, symbol, or other visual/textual marker
     */
    record BoardSquareInfo(Position pos, String label)
            implements GameState {}

    /**
     * Contains the list of players participating in the game along with
     * the index of the currently active player.
     *
     * @param players          ordered list of player descriptors
     * @param activePlayerIdx  index of the active player inside the {@code players} list
     */
    record PlayerListInfo(List<PlayerDesc> players, int activePlayerIdx)
            implements GameState {}

    /**
     * Full information about a single player, suitable for rendering
     * the player's panel or status data.
     *
     * @param name       the player's display name
     * @param gold       amount of gold the player currently has
     * @param hydration  hydration level or similar survival metric
     * @param tools      list of tools currently possessed by this player
     */
    record PlayerInfo(String name, double gold, int hydration, List<String> tools)
            implements GameState {}

    /* ======================= Utility types ======================= */

    /**
     * Represents a column/row position on the game board.
     *
     * @param col zero-based column index
     * @param row zero-based row index
     */
    record Position(int col, int row) {}

    /**
     * Lightweight player descriptor used for board-level views
     * (e.g., showing each player's location on the board).
     *
     * @param name the player's name
     * @param pos  current board position of the player
     */
    record PlayerDesc(String name, Position pos) {}
}
