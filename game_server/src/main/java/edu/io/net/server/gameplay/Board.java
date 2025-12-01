package edu.io.net.server.gameplay;

import edu.io.net.command.UpdateState;
import edu.io.net.command.UpdateStateFactory;
import edu.io.net.command.GameState;

public class Board implements UpdateStateFactory.StateSource {
    private int size = 16;

    public Board() {
        UpdateStateFactory.register(this);
    }

    @Override
    public void populate(UpdateState.Cmd cmd) {
        if (cmd.pack() == GameState.Pack.AFTER_JOIN_GAME) {
            cmd.add(new GameState.BoardInfo(size));
        }
    }
}
