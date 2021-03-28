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
    
    private ServerSocket serverSocket;
    private int numClients;
    
    private ServerSideConnection client1;
    private ServerSideConnection client2;
    
    private String chat = "Hello from server\n";
    
    public Server() {
        numClients = 0;
        try {
            serverSocket = new ServerSocket(25569);
        } catch (IOException e) {
            System.out.println("IOException from server contructor");
        }
    }
    
    public void acceptConnections() {
        try {
            System.out.println("Waiting for connections...");
            while (numClients <  2) {
                Socket s = serverSocket.accept();
                numClients++;
                System.out.println("Client #" + numClients + " has connected");
                ServerSideConnection ssc = new ServerSideConnection(s, numClients);
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
    
    private class  ServerSideConnection implements Runnable {
        
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int clientID;
        
        public ServerSideConnection(Socket socket, int id) {
            this.socket = socket;
            clientID = id;
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
                dataOut.writeInt(clientID);
                dataOut.writeUTF(chat);
                dataOut.flush();
                
                while (true) {     
                    if (clientID == 1) {
                        updateChat("Client #1: " + dataIn.readUTF() + "\n");
                    } else {
                        updateChat("Client #2: " + dataIn.readUTF() + "\n");
                    }
                    client1.sendNewString(chat);
                    client2.sendNewString(chat);
                    System.out.println("Chat is now: \"\n" + chat + "\" chat end.");
                }
            } catch (IOException e) {
                System.out.println("IOException from SSC run() for ID#" + clientID);
            }
        }
        
        public void sendNewString(String msg) {
            try {
                dataOut.writeUTF(msg);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from SSC sendNewString()");
            }
        }
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Hello World: Server");
        Server server = new Server();
        server.acceptConnections();
    }
    
}
