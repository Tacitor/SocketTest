/*
 * Lukas Krampitz
 * Mar 30, 2021
 * 
 */
package krampitzsockettest;

import javax.swing.JOptionPane;

/**
 *
 * @author Tacitor
 */
public class Startup {

    public static void main(String[] args) {
        //get the number of clients the server admin wants
        int numClientsToHave = Integer.parseInt(JOptionPane.showInputDialog("Enter the integer number of clients that will connect:"));

        //create the server socket
        Server server = new Server(numClientsToHave);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //begin listening
                server.acceptConnections();
            }
        });
        t.start();

        /*
        Client client = new Client(700, 200);
        client.connectToServer();
        client.setUpGUI();
        client.setUpButton();
        */

        
        Client clients[] = new Client[numClientsToHave];

        //create the clients
        for (int i = 0; i < numClientsToHave; i++) {
            clients[i] = new Client(700, 200);
            clients[i].connectToServer();
            clients[i].setUpGUI();
            clients[i].setUpButton();
        }
        
    }

}
