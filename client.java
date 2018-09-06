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
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;

/*REFERENCES:
    1. Socket Programming: https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
    2. Base program: http://cs.lmu.edu/~ray/notes/javanetexamples/
    3. 2 Phase Commit Base: https://drive.google.com/drive/folders/0B8CebiqB_IUoQ2RmUy05WkRnUW8
    4. 2 Phase Commit : http://www.oracle.com/technetwork/middleware/ias/how-to-midtier-2pc-088883.html
 */

public class client {

    BufferedReader in;
    private static PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    JButton commit = new JButton("COMMIT");
    JButton abort = new JButton("ABORT");
    JButton globalCommit = new JButton("globalCommit");
    JPanel buttonPanel = new JPanel();
    JPanel subPanel = new JPanel();
    private static ArrayList<String> saveData = new ArrayList<>();
    static Timer timer = new Timer();
    boolean inputFromAll = false;
    boolean commitFlag = false;
    private boolean clicked = false;


    /* Constructs the client by laying out the GUI and registering a
      listener with the textfield so that pressing Return in the
      listener sends the textfield contents to the server.  Note
      however that the textfield is initially NOT editable, and
      only becomes editable AFTER the client receives the NAMEACCEPTED
      message from the server. */

    public client() {

    	/*
         * Layout GUI to give it a display area
         * and a Vote_Request Button.
         */
        buttonPanel.setLayout(new BorderLayout());
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        subPanel.add(commit);
        subPanel.add(abort);
        buttonPanel.add(subPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(buttonPanel, "South");
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
                String httpOutput = textField.getText();
                sdf1 = new SimpleDateFormat("hh:mm:ss");
                Date d = new Date();
                time = sdf1.format(d);

                //Appends the HTTP encoding with the user message
                httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  httpOutput;
                out.println(httpOutput);
                textField.setText("");
            }
        });
        
        /*
         * Set up listener for the Commit button.
         * whenever user clicks "Commit", send 
         * "pre-commit" and "commit" strings alternatively. 
         */
        commit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat sdf1;
                String time;
                sdf1 = new SimpleDateFormat("hh:mm:ss");
                Date d = new Date();
                time = sdf1.format(d);

                //Appends the HTTP encoding with the user message
                if(!clicked){
                	String httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  "PRE_COMMIT";
                	out.println(httpOutput);
                }
                else{
                	String httpOutput = "POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive " + "(" + time + ")" + " " +  "COMMIT";
                	out.println(httpOutput);
                }
                /*
                 * Set up a new timer everytime the commit is clicked.
                 */
                textField.setText("");
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    int time = 40;
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
                        else if(!commitFlag){
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
                clicked = !clicked;
            }
        });
        
        /*
         * Set up listener for the Abort button.
         * whenever user clicks "Commit", send 
         * "ABORT" string to the server.
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
         * Set up listener for the commit button.
         * whenever timer times out, and clients
         * have completed the pre-commit phase,
         * client sends a GLobal Commit String to
         * the server.
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

    /* 
     * Prompt for and return the address of the server. 
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Chatter",
                JOptionPane.QUESTION_MESSAGE);
    }

    /* 
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /* 
     * Connects to the server then enters the processing loop. 
     */
    private void run() throws IOException {

        // Makes the connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        File messages = new File("client.txt");
        PrintStream fileWriter = new PrintStream(new FileOutputStream(messages, true));
        String history;
        try (BufferedReader br = new BufferedReader(new FileReader("client.txt"))) {
            while ((history = br.readLine()) != null) {
                messageArea.append("\n" + history);
            }
        }

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
            } else if (line.startsWith("MESSAGE")) {
                line = line.replace("MESSAGE", "");
                messageArea.append(line + "\n");
                saveData.add(line);
                
                if(line.contains("PRE_COMMIT")){
                    inputFromAll = true;
                }

                /*Client checks if it receives a Global Commit
                  and if it does, then saves the string in
                  non-volatile storage
                */
                else if(line.contains("GLOBAL_COMMIT")){
                    commitFlag = true;
                    for (int j=0; j<saveData.size(); j++){
                        if (!(saveData.get(j).contains("ABORT") || saveData.get(j).contains("COMMIT") || saveData.get(j).contains("Waiting") || saveData.get(j).contains("VOTE_REQUEST") || saveData.get(j).contains("PRE_COMMIT") || saveData.get(j).contains("COMMITTED") || saveData.get(j).contains("ACKNOWLEDGED"))){
                            fileWriter.println(saveData.get(j));
                        }
                    }
                }
                 /*Client checks if it receives a Global Abort
                  and if it does, then discards the string.
                */
                else if (line.contains("GLOBAL_ABORT")){
                	inputFromAll = true;
                    for (int i = 0; i<saveData.size();i++){
                        if (saveData.get(i).contains("aboted")){
                            saveData.remove(i);
                        }
                    }
                }

            }
        }
    }


    /* Runs the client as an application with a closeable frame. */
    public static void main(String[] args) throws Exception {
        client client = new client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}