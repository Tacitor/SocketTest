/*
 * Lukas Krampitz
 * Mar 27, 2021
 * 
 */
package krampitzsockettest;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.*;

/**
 *
 * @author Tacitor
 */
public class Client extends JFrame {

    private int width;
    private int height;
    private Container contentPane;
    private JTextArea header;
    private JTextArea messageRecived;
    private JTextArea messageToSend;
    private JButton sendBtn;

    private int clientID;
    private int otherClient;

    private String chat;
    private boolean buttonEnabled;
    private boolean firstRecive = true;

    private ClientSideConnection csc;

    public Client(int width, int height) {
        this.width = width;
        this.height = height;
        contentPane = this.getContentPane();
        header = new JTextArea();
        messageRecived = new JTextArea();
        messageToSend = new JTextArea();
        sendBtn = new JButton();
    }

    public void setUpGUI() {
        this.setSize(width, height);
        this.setTitle("Catan Socket Test - Client #" + clientID);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.setLayout(new GridLayout(1, 5, 10, 10));
        contentPane.add(header);
        contentPane.add(messageRecived);
        contentPane.add(messageToSend);
        contentPane.add(sendBtn);
        header.setText("Most recent message: ");
        header.setWrapStyleWord(true);
        header.setLineWrap(true);
        header.setEditable(false);
        messageRecived.setWrapStyleWord(true);
        messageRecived.setLineWrap(true);
        messageRecived.setEditable(false);
        messageToSend.setText("Type here...");
        messageToSend.setWrapStyleWord(true);
        messageToSend.setLineWrap(true);
        messageToSend.setEditable(true);
        sendBtn.setText("Send");
        contentPane.setForeground(Color.green);
        contentPane.setBackground(Color.gray);

        if (clientID == 1) {
            header.setText("You are client number 1. Please wait for the rest of the clients to connect before starting\n\nMost recent message: -->");
            //go ahead and wait for the server to send the startup signal
            Thread t = new Thread(new Runnable() {
                public void run() {
                    startUpClient1();
                }
            });
            t.start();
        } else {
            header.setText("You are client number 2. \n\nMost recent message: -->");
            Thread t = new Thread(new Runnable() {
                public void run() {
                    updateTurn();
                }
            });
            t.start();
        }

        buttonEnabled = false;
        updateButtons();
        this.setVisible(true);
    }

    private void connectToServer() {
        csc = new ClientSideConnection();
    }

    public void setUpButton() {
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(messageToSend.getText());

                buttonEnabled = false;
                updateButtons();

                csc.sendNewString(messageToSend.getText());

                Thread t = new Thread(new Runnable() {
                    public void run() {
                        updateTurn();
                    }
                });
                t.start();
            }
        };

        sendBtn.addActionListener(al);
    }

    public void updateButtons() {
        sendBtn.setEnabled(buttonEnabled);
    }
    
    /**
     * Enables the button for client 1 when the server sends the signal
     */
    public void startUpClient1() {
        //place to store the boolean
        //and assign it to the value the server sends
        boolean recivedBoolean = csc.reciveBoolean();
        
        //set the button to the value
        sendBtn.setEnabled(recivedBoolean);
    }

    public void updateTurn() {
        //get the echo from the server with the message just sent
        String msg = csc.reciveNewString();
        messageRecived.setText(msg);

        //if first recive is true then that was not accutaly the echo but that first message
        if (firstRecive && clientID == 2) {
            firstRecive = false;
        } else {

            //wait for newest message from other client
            msg = csc.reciveNewString();
            messageRecived.setText(msg);

        }

        buttonEnabled = true;
        updateButtons();
    }

    //client connection inner class
    private class ClientSideConnection {

        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection() {
            System.out.println("----Client----");
            try {
                socket = new Socket("99.225.194.200", 25569);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                clientID = dataIn.readInt();
                chat = dataIn.readUTF();
                System.out.println("Connected to a server as Client #" + clientID);
                messageRecived.setText(chat);
            } catch (IOException e) {
                System.out.println("IOException from CSC contructor ");
            }
        }

        public void sendNewString(String mesg) {
            try {
                dataOut.writeUTF(mesg);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from CSC sendNewString()");
            }
        }

        public String reciveNewString() {
            String msg = "";

            try {
                msg = dataIn.readUTF();
            } catch (IOException ex) {
                System.out.println("IOException from CSC reciveNewString()");
            }

            return msg;
        }
        
        public boolean reciveBoolean() {
            boolean bool = false;
            
            try {
                bool = dataIn.readBoolean();
            } catch (IOException ex) {
                System.out.println("IOException from CSC reciveBoolean()");
            }
            
            return bool;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Hello World: Client");
        Client client = new Client(700, 200);
        client.connectToServer();
        client.setUpGUI();
        client.setUpButton();
    }

}
