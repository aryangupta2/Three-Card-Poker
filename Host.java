/*
Purpose: Creates server for players to join	
Contributors: Raman
*/


import java.io.IOException;	
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Host {
    // Instance field
    private final ServerSocket listener;    // Listens for any players wanting to join
    private int playerCount = 0;            // Keeps track of the number of players
    private boolean started = false;        // Stores whether host has started game or not

    public Host(ServerSocket serverSocket) {
        this.listener = serverSocket;
    }

    public void startServer() {
    	Poker game = new Poker();
        try {
            Scanner input = new Scanner(System.in);

            // Listnes on port 9090 for any new players
            while (!listener.isClosed() && !started) {
                System.out.println("Waiting for players to join...");
                Socket player = listener.accept();
                System.out.println("A new player has joined..");

                PlayerHandler playerHandler = new PlayerHandler(player);
                game.addPlayer(playerHandler);

                Thread playerThread = new Thread(playerHandler);
                playerThread.start();
                playerCount++;

                if (playerCount >= 3 && playerCount < 17) {
                    System.out.print("Start game (Y/N): ");
                    String choice = input.nextLine();
                    if (choice.equalsIgnoreCase("Y")) started = true;
                }

                if (playerCount == 17) {
                    System.out.println("Max # of player accepted. The game will start now.");
                    started = true;
                }
            }
        } catch (IOException e) {closeServer();}

        PlayerHandler.gameStarted = true;
        
        while (game.getPlayerCount() > 2) {
        	
        }
    }

    // Shuts down server
    public void closeServer() {
        try {
            if (listener != null) listener.close();
        } catch (IOException e) {e.printStackTrace();}
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9090);
        Host server = new Host(serverSocket);
        server.startServer();
    }

}
