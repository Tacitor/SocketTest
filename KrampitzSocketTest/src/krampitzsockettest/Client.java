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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
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
    private JButton fileBtn;

    private int clientID;
    private int totalClientNum; //the number of total clients that will be connected to the server
    private int messagesRecivedThisCyle;

    private String chat;
    private boolean buttonEnabled;
    private boolean justPressedSend = false; //if this client waiting for the first transmision from the server
    private boolean firstRecive = true;

    private ClientSideConnection csc; //the socket type var to hold the connection for this Client

    /**
     * Constructor
     *
     * @param width
     * @param height
     */
    public Client(int width, int height) {
        /* Set the Windows 10 look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading Windows Look and feel");
        }
        
        
        this.width = width;
        this.height = height;
        contentPane = this.getContentPane();
        header = new JTextArea();
        messageRecived = new JTextArea();
        messageToSend = new JTextArea();
        sendBtn = new JButton();
        fileBtn = new JButton();

        //no messages have been recived yet
        messagesRecivedThisCyle = 0;
    }

    public void setUpGUI() {
        //get up the GUI
        this.setSize(width, height);
        this.setTitle("Catan Socket Test - Client #" + clientID);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.setLayout(new GridLayout(1, 5, 10, 10));
        contentPane.add(header);
        contentPane.add(messageRecived);
        contentPane.add(messageToSend);
        contentPane.add(sendBtn);
        contentPane.add(fileBtn);
        header.setText("Most recent message: ");
        header.setWrapStyleWord(true);
        header.setLineWrap(true);
        header.setEditable(false);
        header.setFont(new Font("Arial", Font.PLAIN, 12));
        messageRecived.setWrapStyleWord(true);
        messageRecived.setLineWrap(true);
        messageRecived.setEditable(false);
        messageRecived.setFont(new Font("Arial", Font.PLAIN, 12));
        messageToSend.setText("Type here...");
        messageToSend.setWrapStyleWord(true);
        messageToSend.setLineWrap(true);
        messageToSend.setEditable(true);
        messageToSend.setFont(new Font("Arial", Font.PLAIN, 12));
        sendBtn.setText("Send Chat");
        fileBtn.setText("Send File");
        contentPane.setForeground(Color.green);
        contentPane.setBackground(Color.gray);

        //specific behaviour for the client numbers
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
            header.setText("You are client number " + clientID + ". \n\nMost recent message: -->");
            //wait for a message to come through
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
        //set up the socket
        csc = new ClientSideConnection();
    }

    public void setUpButton() {
        //create action listener for when the button is clicked to send a message
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();
                String buttonString = button.getText();

                //if the player sends a chat
                if (buttonString.equals("Send Chat")) {

                    justPressedSend = true;
                    //new cycle for this client, so set it to 0
                    messagesRecivedThisCyle = 0;

                    System.out.println("Sending the message: " + messageToSend.getText());

                    buttonEnabled = false;
                    updateButtons();

                    //send the message
                    csc.sendNewString(messageToSend.getText());

                    //clear the chat field
                    messageToSend.setText("");

                    //now wait fot a response
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            updateTurn();
                        }
                    });
                    t.start();
                } else if (buttonString.equals("Send File")) {

                    JFileChooser saveFileLoader = new JFileChooser();
                    //set up the file choose and call it
                    saveFileLoader.setDialogTitle("Select a Save File to Open:");
                    int userLoadSelection = saveFileLoader.showOpenDialog(null);

                    if (userLoadSelection == JFileChooser.APPROVE_OPTION) {

                        buttonEnabled = false;
                        updateButtons();

                        //new cycle for this client, so set it to 0
                        messagesRecivedThisCyle = 0;

                        //test if it is a vailid save file
                        try {
                            String filePath = saveFileLoader.getSelectedFile().getPath();
                            File file = new File(filePath);
                            Scanner scanner = new Scanner(file);
                            FileInputStream fileStream = new FileInputStream(file);

                            int fileLength = (int) file.length();

                            String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);

                            //debug file name and length
                            //System.out.println(fileName);
                            //System.out.println(fileLength);
                            byte fileBytes[] = new byte[fileLength];
                            fileStream.read(fileBytes, 0, fileLength);

                            //debug the stream
                            //System.out.println(Arrays.toString(fileBytes));
                            csc.sendFileStream(fileBytes); //send the file

                            //clear the chat field
                            messageToSend.setText("");

                            justPressedSend = true;

                            //now wait fot a response
                            Thread t = new Thread(new Runnable() {
                                public void run() {
                                    updateTurn();
                                }
                            });
                            t.start();

                        } catch (FileNotFoundException exception) {
                            JOptionPane.showMessageDialog(null, "There was an error loading the save file:\n" + exception, "Loading Error", JOptionPane.ERROR_MESSAGE);
                        } catch (IOException exception) {
                            JOptionPane.showMessageDialog(null, "There was an IOException loading the save file:\n" + exception, "Loading Error", JOptionPane.ERROR_MESSAGE);
                        }

                    }
                }
            }
        };

        sendBtn.addActionListener(al);
        fileBtn.addActionListener(al);
    }

    public void updateButtons() {
        sendBtn.setEnabled(buttonEnabled);
        fileBtn.setEnabled(buttonEnabled);
    }

    /**
     * Enables the button for client 1 when the server sends the signal
     */
    public void startUpClient1() {
        //doesn't need a first recive
        firstRecive = false;

        //place to store the boolean
        //and assign it to the value the server sends
        boolean recivedBoolean = csc.reciveBoolean();

        //set the button to the value
        buttonEnabled = recivedBoolean;
        updateButtons();
    }

    public void updateTurn() {
        int messagesNeededThisCycle;

        if (firstRecive) {
            messagesNeededThisCycle = (clientID - 1);
            firstRecive = false;
        } else {
            messagesNeededThisCycle = (totalClientNum - 1);
        }

        //only update the clients turn if all other clients have had a go
        while (messagesRecivedThisCyle < messagesNeededThisCycle) {

            //if the client is just listening for 1 response
            if (!justPressedSend) {

                regularRecive();

            } else { //or if the client just sent a message and expects and echo and then a response

                //get the type of message the server is sending
                int type = csc.reciveType();

                //get the echo from the server with the message just sent
                if (type == 1) {
                    String msg = csc.reciveNewString();
                    messageRecived.setText(msg);

                    //then to a regular recive
                    regularRecive();

                } else if (type == 2) { //if file is being sent
                    //revice the file using the special Object that carreis the file and the chat
                    FileTypeRecieve fileTypeRecieve = csc.recieveFile();
                    //update the chat
                    messageRecived.setText(fileTypeRecieve.getChat());

                    //then do nothing with the file because. Since this client just pressed the button it doesn't need the file
                    //debug the file and how it was recived
                    //System.out.println("Special Got file:\n" + Arrays.toString(fileTypeRecieve.getFile()));
                    //then to a regular recive
                    regularRecive();
                }

                //the special case for clients that just sent a message has run. Now it needs to listen
                justPressedSend = false;

            }

            messagesRecivedThisCyle++;

        }

        updateButtons();

    }

    private void regularRecive() {
        int type = csc.reciveType();

        if (type == 1) {
            //wait for newest message from other client
            String msg = csc.reciveNewString();
            messageRecived.setText(msg);

            checkForTurn(msg);

        } else if (type == 2) { //else if type is 2

            FileTypeRecieve fileTypeRecieve = csc.recieveFile();
            messageRecived.setText(fileTypeRecieve.getChat());
            //debug the file and how it was recived
            //System.out.println("Regular Got file:\n" + Arrays.toString(fileTypeRecieve.getFile()));

            //write the file
            try {
                String saveToPath = System.getProperty("user.home")
                        + File.separator + "AppData" + File.separator + "Roaming" + File.separator + "SettlerDevs" + File.separator + "NetworkTest"
                        + File.separator + "Client" + clientID;

                //ensure the directory is there
                Files.createDirectories(Paths.get(saveToPath));

                //Create and output stream at the directory
                FileOutputStream fos = new FileOutputStream(saveToPath + File.separator + "file.txt");

                //write the file
                fos.write(fileTypeRecieve.getFile(), 0, fileTypeRecieve.getFile().length);
            } catch (FileNotFoundException exception) {
                JOptionPane.showMessageDialog(null, "There was an error loading the save file:\n" + exception, "Loading Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null, "There was an IOException loading the save file:\n" + exception, "Loading Error", JOptionPane.ERROR_MESSAGE);
            }
            //System.out.println("Chat is : \n" + fileTypeRecieve.getChat());
            checkForTurn(fileTypeRecieve.getChat());

        } else {
            buttonEnabled = false;
        }
    }

    public void checkForTurn(String msg) {
        //check if that transmision was the person right before me
        //System.out.println(msg.lastIndexOf("#"));
        //System.out.println(msg.charAt(msg.lastIndexOf("#") + 1));
        //check if that message just came from the client infront of this one
        if (Integer.parseInt(Character.toString(msg.charAt(msg.lastIndexOf("#") + 1))) == (clientID - 1)) {
            //System.out.println("True " + clientID);
            buttonEnabled = true;
        } else {
            //if it wasn't from the one infront check if it came from the last one only if this is the first one
            if (clientID == 1 && Integer.parseInt(Character.toString(msg.charAt(msg.lastIndexOf("#") + 1))) == totalClientNum) {
                //System.out.println("True " + clientID);
                buttonEnabled = true;
            } else {

                //System.out.println("False " + clientID);
                buttonEnabled = false;
            }
        }
    }

    private class FileTypeRecieve {

        private byte[] file;
        private String chat;

        public FileTypeRecieve() {
            this.file = new byte[1];
            this.chat = "";
        }

        public FileTypeRecieve(byte[] file, String chat) {
            this.file = file;
            this.chat = chat;
        }

        public String getChat() {
            return chat;
        }

        public byte[] getFile() {
            return file;
        }

    }

    //client connection inner class
    private class ClientSideConnection {

        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection() {
            System.out.println("----Client----");
            try {
                //establic connection
                socket = new Socket("99.225.194.200", 25569);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                //now that a connection has been establichsed get the number for this client
                clientID = dataIn.readInt();
                //the the totalClientNum
                totalClientNum = dataIn.readInt();
                //get the starting chat
                chat = dataIn.readUTF();
                System.out.println("Connected to a server as Client #" + clientID);
                messageRecived.setText(chat);
            } catch (IOException e) {
                System.out.println("IOException from CSC contructor ");
            }
        }

        public void sendNewString(String mesg) {
            try {
                dataOut.writeInt(1); //tell the server that is it recieving a chat message
                dataOut.writeUTF(mesg);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from CSC sendNewString()");
            }
        }

        public void sendFileStream(byte[] fileStream) {
            try {
                dataOut.writeInt(2); //tell the server that is it recieving a file
                dataOut.writeInt(fileStream.length); //send the length of the file
                dataOut.write(fileStream, 0, fileStream.length);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from CSC sendFileStream()");
            }
        }

        public FileTypeRecieve recieveFile() {
            String msg = "";
            byte[] file = new byte[1];

            try {
                msg = dataIn.readUTF();
                //get the file length
                file = new byte[dataIn.readInt()];
                //get the file
                dataIn.read(file, 0, file.length);
            } catch (IOException ex) {
                System.out.println("IOException from CSC reciveNewString()");
            }

            return new FileTypeRecieve(file, msg);
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

        public int reciveType() {
            int msg = 0;

            try {
                msg = dataIn.readInt();
            } catch (IOException ex) {
                System.out.println("IOException from CSC reciveType()");
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
