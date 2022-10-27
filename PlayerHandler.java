/*
Purpose:		
Create multiple threads for each player that can run in parallel.
Also stores player data and holds some game functionality.

Contributors: Aryan, Harjevan, and Raman
 */


import java.io.*;	
import java.net.Socket;
import java.util.ArrayList;

public class PlayerHandler implements Runnable {
    // Server fields
    private Socket socket;
    private BufferedReader fromPlayer;
    private BufferedWriter toPlayer;

    // User data
    private int id;                 // Unique user id
    private String username;        // Player's user name
    private double balance = 12;    // How much money the player has left
    public Card[] hand;             // Player's hand
    private boolean folded = false; // Stores whether user has folded or not
    PlayerHandler link;             // Holds link to next player

    public static ArrayList<PlayerHandler> players = new ArrayList<>(); // List of players
    public static volatile boolean gameStarted;     // Tracks whether game has started or not


    // Initializes player
    public PlayerHandler(Socket playerSocket) {
        try {
            this.socket = playerSocket;
            fromPlayer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toPlayer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            this.username = fromPlayer.readLine();
            this.id = players.size() + 1;
            players.add(this);

            broadcastMsg("[HOST]: " + username + " has joined the game!");
        } catch (IOException e) {
            closeEverything(socket, fromPlayer, toPlayer);
        }
    }

    // Shuts down player-sever connection
    public void closeEverything(Socket socket, BufferedReader in, BufferedWriter out) {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sends message to all players excluding the implicit player
    public void broadcastMsg(String msg) {
        if (msg == null) return;
        for (PlayerHandler player : players) {
            try {
                if (!player.username.equals(this.username)) {
                    player.toPlayer.write(msg);
                    player.toPlayer.newLine();
                    player.toPlayer.write("> ");
                    player.toPlayer.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, fromPlayer, toPlayer);
            }
        }
    }

    // Sends message to only the implicit player
    public void writeToPlayer(String msg) throws IOException {
        toPlayer.write(msg);
        toPlayer.newLine();
        toPlayer.write("> ");
        toPlayer.flush();
    }

    public void showHand() throws IOException {
        String[] n = new String[3];
        String[] s = new String[3];

        for (int i = 0; i < this.hand.length; i++) {
            n[i] = Integer.toString(this.hand[i].getNumber());
            if (n[i].equals("1")) n[i] = "A";
            if (n[i].equals("11")) n[i] = "J";
            if (n[i].equals("12")) n[i] = "Q";
            if (n[i].equals("13")) n[i] = "K";

            s[i] = this.hand[i].getSuit();
        }

        toPlayer.newLine();
        toPlayer.write("---------\t---------\t---------");
        toPlayer.newLine();
        toPlayer.write("|       |\t|       |\t|       |");
        toPlayer.newLine();


        toPlayer.write("|  " + n[0] + " " + s[0] + "  |\t" +
                "|  " + n[1] + " " + s[1] + "  |\t" +
                "|  " + n[2] + " " + s[2] + "  |");
        toPlayer.newLine();

        toPlayer.write("|       |\t|       |\t|       |");
        toPlayer.newLine();
        toPlayer.write("---------\t---------\t---------");
        toPlayer.newLine();
        toPlayer.flush();
    }

    // Gets user choice at the beginning of each round
    public int getChoice() throws IOException {
        printMenu();

        String choice = fromPlayer.readLine();
        return Integer.parseInt(choice);
    }

    // Print list of choices
    public void printMenu() {
        try {
            toPlayer.write("[1] See your hand");
            toPlayer.newLine();
            toPlayer.write("> [2] Continue to next round");
            toPlayer.newLine();
            toPlayer.write("> [3] Fold and leave the game");
            toPlayer.newLine();
            toPlayer.write("> Note: A number outside the range will result in folding from the round");
            toPlayer.newLine();
            toPlayer.write("> Choice:");
            toPlayer.newLine();
            toPlayer.flush();
        } catch (IOException e) {
            closeEverything(socket, fromPlayer, toPlayer);
        }

    }

    // Updates user balance
    public int updateBalance(double x) {
        double temp = balance;
        temp -= x;
        if (temp<=0){
            return -1;
        }
        else {
            balance = temp;
            return 0;
        }
    }


    // Accessors
    public int getID() {
        return this.id;
    }

    public double getBalance() {
        return this.balance;
    }
    
    public String getUserName() {
    	return this.username;
    }


    @Override
    public void run() {
        while (socket.isConnected() && !gameStarted) Thread.onSpinWait();

        // Starts new game
        Poker game = new Poker();
        game.addPlayer(this);

        try {
            writeToPlayer("The game has begun!");

            writeToPlayer("Shuffling cards...");

            writeToPlayer("Dealing cards...");
        } catch (IOException e) {
            closeEverything(socket, fromPlayer, toPlayer);
        }
        game.dealCards();

        // Main game loop
        while (socket.isConnected() && !folded) {
        	String list = game.printPlayerHandler();
        	System.out.println(list);
        	
            if (game.getPlayerCount() == 2) {
            	System.out.println("here");
                int winnerID = Poker.determineWinner(game.getHead(), game.getHead().link);

                if (winnerID != -1) {
                    PlayerHandler winner = game.findPlayer(winnerID);
                    String winnerName = winner.username;
                    winner.updateBalance(game.getPot());

                    broadcastMsg("[HOST]: " + winnerName + " has won the game!");
                    broadcastMsg("[HOST]: " + winnerName + " made $" + game.getPot());
                }
                else {
                    PlayerHandler winner1 = game.getHead();
                    PlayerHandler winner2 = game.getHead().link;

                    String winner1Name = winner1.username;
                    String winner2Name = winner2.username;

                    double split = Math.round((game.getPot() / 2.0) * 100) / 100.0;

                    broadcastMsg("[HOST]: " + winner1Name + " & " + winner2Name + " tied");
                    broadcastMsg("[HOST]: " + winner1Name + " made $" + split);
                    broadcastMsg("[HOST]: " + winner2Name + " made $" + split);
                }
            }


            // Gets player choice
            int choice = 0;
            try {
                choice = getChoice();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                switch (choice) {
                    case 1 -> {  // Show user their cards
                        showHand();
                    }
                    case 2 -> { // Update users balance and mve them onto the next round
                        updateBalance(game.getGameFee() * -1);
                        game.addToPot(game.getGameFee());
                        broadcastMsg("[HOST]: " + this.username + " is going to the next round!");
                    }
                    case 3 -> { // Remove the player from game
                        game.removePlayer(this.id);
                        players.remove(this);
                        broadcastMsg("[HOST]: " + this.username + " folded...");
                        folded = true;
                        writeToPlayer("[HOST]: Closing connection...");
                        closeEverything(socket, fromPlayer, toPlayer);
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, fromPlayer, toPlayer);
            }

            // add some sort of way to halt program till everyone has finished making a decision
        }
    }
}
