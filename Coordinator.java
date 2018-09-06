/*Submitted By:
        LAKSHAY SHARMA
        ID - 1001551210
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;
import java.util.logging.Handler;

import javax.swing.*;

/*REFERENCES:
    1. Socket Programming: https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
    2. Base program: http://cs.lmu.edu/~ray/notes/javanetexamples/
    3. 2 Phase Commit Base: https://drive.google.com/drive/folders/0B8CebiqB_IUoQ2RmUy05WkRnUW8
    4. 2 Phase Commit : http://www.oracle.com/technetwork/middleware/ias/how-to-midtier-2pc-088883.html
 */

public class Coordinator {

    /*
        List of all the client names, handlers and data-sets
        necessary for processing the messages according to
        2 Phase COMMIT
     */
    private static ArrayList<String> tempData = new ArrayList<>();
    ArrayList<String> data = ChatServer.data;
    ArrayList<ChatServer.Handler> handlers = ChatServer.handlers;
    HashSet<PrintWriter> writers= ChatServer.writers;
    HashSet<String> name = ChatServer.names;
    String[] httpParts;
    PrintWriter writer;
    boolean inputFromAll = false;
    boolean commitDec = false;

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    JButton vote = new JButton("Request Vote");
    JButton abort = new JButton("Abort");
    JButton globalCommit = new JButton("GLOBAL_COMMIT");
    static Timer timer;



    /* Constructs the client by laying out the GUI and registering a
      listener with the textfield so that pressing Return in the
      listener sends the textfield contents to the server.  Note
      however that the textfield is initially NOT editable, and
      only becomes editable AFTER the client receives the NAMEACCEPTED
      message from the server. */

    ChatServer chatServer = new ChatServer();
    HashSet<String> names = chatServer.getList();

    public Coordinator() {

        /*
         * Layout GUI to give it a display area
         * and a Vote_Request Button.
         */
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.getContentPane().add(vote, "South");
        frame.pack();

        /*
         * Add listener for the text area.
         * if the user types a string and press
         * "Enter", send the string to the server
         */
        textField.addActionListener(new ActionListener() {

            /** Responds to pressing the enter key in the textfield by sending
             the contents of the text field to the server.    Then clear
             the text area in preparation for the next message.
             Also, concatenates the HTTP encoding with the user input each time. */

            public void actionPerformed(ActionEvent e) {

                //Calculates the current system time to send it along with the user message
                SimpleDateFormat sdf1;
                String time;
                String userout = textField.getText();
                sdf1 = new SimpleDateFormat("hh:mm:ss");
                Date d = new Date();
                time = sdf1.format(d);

                //Appends the HTTP encoding with the user message
                 String httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  userout;
                out.println(httpOutput);
                textField.setText("");
                /*
                 * Set up a new timer everytime the vote-button is clicked.
                 */
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    int time = 25;
                    @Override
                    public void run() {
                        time--;
                        /*
                         * check whether the pre-commit phase is complete
                         * and if it is, stop the timer, otherwise send
                         * Abort message to the clients.
                         */
                        if (!inputFromAll){
                            if(time < 0){
                                System.out.println("Timeout");
                                abort.doClick();
                                timer.cancel();
                                timer.purge();
                            }
                        }
                        /*
                         * check whether the Global commit phase is complete,
                         * and if it is, stop the timer, else if pre-commit 
                         * phase is complete, send a Global Commit to all
                         * the clients.
                         */
                        else if(!commitDec){
                        		if(time<0){
	                        		System.out.println("Global Commit");
	                        		globalCommit.doClick();
			                        timer.cancel();
			                        timer.purge();
                        	}
                        }
                    }
                    /*
                     * timer runs in a cycle with a 1 second 
                     * interval.
                     */
                },1000,1000);

            }
        });

        vote.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	
            	//Calculates the current system time to send it along with the user message
                SimpleDateFormat sdf1;
                String time;
                sdf1 = new SimpleDateFormat("hh:mm:ss");
                Date d = new Date();
                time = sdf1.format(d);
                String request = "VOTE_REQUEST";
                
                //Appends the HTTP encoding with the user message
                String httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  request;
                out.println(httpOutput);
                textField.setText("");
            }
        });
        
        /*
         * Abort button setup to send an HTTP encoded
         * "Abort" message if timer times out. The button 
         * is set visible on the GUI.
         */
        abort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat sdf1;
                String time;
                sdf1 = new SimpleDateFormat("hh:mm:ss");
                Date d = new Date();
                time = sdf1.format(d);

                //Appends the HTTP encoding with the user message
                String httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  "ABORT";
                out.println(httpOutput);
                textField.setText("");
            }
        });
        
        /*
         * Commit button setup to send an HTTP encoded
         * "GLobal Commit" message if the timer times out. 
         * The button is set visible on the GUI
         */
        globalCommit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat sdf1;
                String time;
                sdf1 = new SimpleDateFormat("hh:mm:ss");
                Date d = new Date();
                time = sdf1.format(d);

                //Appends the HTTP encoding with the user message
                String httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  "GLOBAL_COMMIT";
                out.println(httpOutput);
                textField.setText("");
            }
        });
    }

    private void sendText(String a){
        out.println(a);
    }

    /* Prompt for and return the address of the server. */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Chatter",
                JOptionPane.QUESTION_MESSAGE);
    }

    /* Prompt for and return the desired screen name.*/
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /* Connects to the server then enters the processing loop. */
    private void run() throws IOException {

        // Makes the connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        /* Processes all messages from server, according to the protocol and
           gets the system time and sends it along with the processed input. */
        while (true) {
                String line = in.readLine();
                /*
                 * GUI pop up to submit username.
                 */
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                    
                } else if (line.startsWith("NAMEACCEPTED")) {
                    textField.setEditable(true);
                }
                else if (line.startsWith("MESSAGE")){
                    line = line.replace("MESSAGE", "");

                    /* Checks what the server echoes back
                       takes decision accordingly*/
                    
                    
                    if (line.equals(" GLOBAL_ABORT")) {
                        messageArea.append("GLOBAL_ABORT \n");
                    }
                    else if (line.contains("PRE_COMMIT")) {
                        messageArea.append("PRE_COMMIT Phase Complete.... \nSending Next ACK....");
                        inputFromAll = true;
                        vote.doClick();
                    }
                    else if (line.contains("GLOBAL_COMMIT")){
                    	commitDec = true;
                    	messageArea.append("\nGLOBAL_COMMIT.... \nWriting to log....");
                    }
                }
        }
    }

    /* Runs the client as an application with a closeable frame. */
    public static void main(String[] args) throws Exception {
        Coordinator coordinator = new Coordinator();
        coordinator.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        coordinator.frame.setVisible(true);
        coordinator.run();
    }
}