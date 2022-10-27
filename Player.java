/*
Purpose: Connect player to server
Contributors: Raman
*/

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Player {
    private Socket socket;
    private BufferedReader fromHost;
    private BufferedWriter toHost;
    private String userName;

    // Initializes player
    public Player(Socket socket, String userName) {
        try {
            this.socket = socket;
            this.userName = userName;
            fromHost = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toHost = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeEverything(socket, fromHost, toHost);
        }
    }

    // Send messages to the server
    public void sendMsgs() {
        try {
            toHost.write(userName);
            toHost.newLine();
            toHost.flush();

            Scanner in = new Scanner(System.in);
            while (socket.isConnected()) {
                System.out.print("> ");
                String msg = in.nextLine();
                toHost.write(msg);
                toHost.newLine();
                toHost.flush();
            }

        } catch (IOException e) {
            closeEverything(socket, fromHost, toHost);
        }
    }

    // Receive message from the server
    public void listenToMsgs() {
        new Thread(() -> {
            try {
                String msgFromHost = " ";
                while (socket.isConnected() && msgFromHost != null) {
                    msgFromHost = fromHost.readLine();
                    System.out.println(msgFromHost);
                }
            } catch (IOException e) {
                closeEverything(socket, fromHost, toHost);
            }
        }).start();
    }

    // Shut down player-server connection
    public void closeEverything(Socket socket, BufferedReader in, BufferedWriter out) {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {e.printStackTrace();}
    }


    public static void main(String[] args) throws IOException {
        // Game intro from here
        Scanner input = new Scanner(System.in);
        String username, SERVER_IP;
        int SERVER_PORT;

        System.out.print("Enter username for game: ");
        username = input.nextLine();

        System.out.print("Enter IP address of server: ");
        SERVER_IP = input.nextLine();

        System.out.print("Enter PORT number: ");
        SERVER_PORT = Integer.parseInt(input.nextLine());

        System.out.println("Wait for host to start the game...");
        // To here

        // Server Stuff
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        Player player = new Player(socket, username);

        player.listenToMsgs();
        player.sendMsgs();
    }
}