/*
 * Lukas Krampitz
 * Mar 27, 2021
 * 
 */
package krampitzsockettest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Tacitor
 */
public class Server {

    //The reciving socket
    private ServerSocket serverSocket;
    //the number of clients what have connected
    private int numClients;

    //The array of clients
    private ServerSideConnection client1;
    private ServerSideConnection client2;

    //A starting String
    private String chat = "Hello from server\n";

    public Server() {
        //no clients have connected yet
        numClients = 0;
        //create the socket to listen 
        try {
            serverSocket = new ServerSocket(25569);
        } catch (IOException e) {
            System.out.println("IOException from server contructor");
        }
    }

    public void acceptConnections() {
        try {
            System.out.println("Waiting for connections...");
            //wait until all the clients have connected
            while (numClients < 2) {
                //create a reciving socket on the server side
                Socket s = serverSocket.accept();
                //count it as a client
                numClients++;
                System.out.println("Client #" + numClients + " has connected");
                //create a new SSC for to keep track of that incoming socket
                ServerSideConnection ssc = new ServerSideConnection(s, numClients);
                //save that new ssc to the list of clients
                if (numClients == 1) {
                    client1 = ssc;
                } else {
                    client2 = ssc;
                }
                //start a new thread just for that one client
                Thread t = new Thread(ssc);
                t.start();
            }
            System.out.println("We now have two players. No more connections will be accepted.");
        } catch (IOException e) {
            System.out.println("IOException from acceptConnections");
        }
    }

    private void updateChat(String newMsg) {
        chat += newMsg;
    }

    private void clearChat() {
        chat = "";
    }

    private class ServerSideConnection implements Runnable {

        private Socket socket; //the socket that this client connected with
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int clientID;

        /**
         * Constructor
         *
         * @param socket
         * @param id
         */
        public ServerSideConnection(Socket socket, int id) {
            this.socket = socket;
            clientID = id;
            //setup the data streams
            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("IOException from SSC constuctor");
            }
        }

        @Override
        public void run() {
            try {
                //when a client first connects send it's ID and the chat in it's current state
                dataOut.writeInt(clientID);
                dataOut.writeUTF(chat);
                dataOut.flush(); //send it

                //check if this is the last client to connect
                if (clientID == 2) {
                    //then tell the first client to begin
                    client1.sendBoolean(true);
                    
                    System.out.println("Send begin command to Client 1");
                }

                //loop state after all startup business is complete
                while (true) {
                    //accept a message
                    String newMsg = dataIn.readUTF();

                    //check if a user wants to clear the chat
                    if (newMsg.equals("/clear")) {
                        clearChat();

                    } else { //else add the new string

                        //if a message come from client 1
                        if (clientID == 1) {
                            updateChat("Client #1: " + newMsg + "\n");
                        } else { //if it instead comes from client 2
                            updateChat("Client #2: " + newMsg + "\n");
                        }
                    }
                    //send the new chat out to all the clients
                    client1.sendNewString(chat);
                    client2.sendNewString(chat);
                    //debug the chat
                    //System.out.println("Chat is now: \"\n" + chat + "\" chat end.");
                }
            } catch (IOException e) {
                System.out.println("IOException from SSC run() for ID#" + clientID);
            }
        }

        /**
         * Send a string to the client
         * 
         * @param msg 
         */
        public void sendNewString(String msg) {
            try {
                dataOut.writeUTF(msg);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from SSC sendNewString()");
            }
        }

        /**
         * Send a boolean to the client
         * 
         * @param msg 
         */
        public void sendBoolean(boolean msg) {
            try {
                dataOut.writeBoolean(msg);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from SSC sendBoolean()");
            }
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Hello World: Server");
        //create the server socket
        Server server = new Server();
        //begin listening
        server.acceptConnections();
    }

}
