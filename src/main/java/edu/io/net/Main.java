package edu.io.net;

import edu.io.net.command.Handshake;
import edu.io.net.command.JoinGame;


public class Main {
    public static void main(String[] args) {
        var connection = new GameServerConnector("tcp://localhost:1313", new SocketConnector());
        
        connection.connect();

        if(!connection.isConnected()) {
            System.out.println("I cant connect.");
            return;
        }
        
        connection.issueCommand(new Handshake.Cmd("1.1.13"), (connected) ->{
            System.out.println("Handshake complete: ");
        });

        connection.issueCommand(new JoinGame.Cmd("player"));
        
    }
}
