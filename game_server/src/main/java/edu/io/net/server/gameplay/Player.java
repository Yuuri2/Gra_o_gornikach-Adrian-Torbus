package edu.io.net.server.gameplay;

import edu.io.net.command.JoinGame;

import java.util.Objects;

public class Player {
    private String name;

    public Player(JoinGame.Cmd cmd) {
        Objects.requireNonNull(cmd);
        name = cmd.name;
    }

    public String name() {
        return name;
    }

}
