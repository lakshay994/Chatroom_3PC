/*Submitted By:
        LAKSHAY SHARMA
        ID - 1001551210
 */

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/*REFERENCES:
    1. Socket Programming: https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
    2. Base program: http://cs.lmu.edu/~ray/notes/javanetexamples/
    3. 2 Phase Commit Base: https://drive.google.com/drive/folders/0B8CebiqB_IUoQ2RmUy05WkRnUW8
    4. 2 Phase Commit : http://www.oracle.com/technetwork/middleware/ias/how-to-midtier-2pc-088883.html
 */

public class ChatServer {


    /*
        List of threads and data sets which will be used at
        the Coordinator in order to process the requests.
     */
   public static ArrayList<String> data = new ArrayList<>();
   public static ArrayList<String> commitData = new ArrayList<>();
   public static ArrayList<Handler> handlers = new ArrayList<>();
   private static ArrayList<String> tempData = new ArrayList<>();

    //GUI setup
    private static JFrame chatServer = new JFrame("Chat Server");
    private static JTextArea messageArea = new JTextArea(20, 40);

    // The port that the server listens on.
    private static final int PORT = 9001;

    /* The set of all names of clients and their corresponding outputstreams in the chat room.
       Maintained so that we can check that new clients are not registering name
       already in use. */
    public static HashSet<String> names = new HashSet<String>();

    public static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    private static String line = null;

    /* The application main method, which just listens on a port and spawns handler threads. */
    public static void main(String[] args) throws Exception {
    	
    	/*
    	 * Setting Up the Server GUI
    	 * to display all the messages
    	 * that the Server is relaying
    	 */
        chatServer.getContentPane().add(new JScrollPane(messageArea), "Center");
        chatServer.pack();
        ChatServer.chatServer.setVisible(true);

        messageArea.append("The chat server is running.");
        
        //Checks if there already exists a saved chat file and if its empty or not.
        File history = new File("messages.txt");
        if(history.length() > 0){
           
        	//If file is not empty then display the contents of the file.
            messageArea.append("\nLoading Previous Chat History.......");
            try (BufferedReader br = new BufferedReader(new FileReader("messages.txt"))) {
                
            	//Appending all the contents of the history file to the GUI
            	while ((line = br.readLine()) != null) {
                    messageArea.append("\n" + line);
                }
            }
        }
        else {
            messageArea.append("\nNo Chat History as of yet....");
        }
        
        //Setting up and connecting to the Server via the specified PORT
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
               
            	/*
            	 * Set up a arraylsit of 
            	 * all the threads based on all the
            	 * clients that join in.
            	 */
            	Handler handler = new Handler(listener.accept());
                handler.start();
                handlers.add(handler);
            }
        } finally {
            listener.close();
        }

    }

    // Handler class to handle all the clients that connect to the server.
    public static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        // Handler constructor
        public Handler(Socket socket) {
            this.socket = socket;
        }

        // run method takes care of all the actual processing of the client thread.
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                File messages = new File("messages.txt");
                PrintStream fileWriter = new PrintStream(new FileOutputStream(messages, true));
                String newString;
                boolean inputFromAll = false;
                boolean commitFlag = false;
                boolean abortFlag = false;
                boolean coord = false;
               
                
                /* Request a name from this client.  Keep requesting until
                   a name is submitted that is not already used.  Note that
                   checking for the existence of a name and adding the name
                   must be done while locking the set of names. */
                
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            if (!name.equalsIgnoreCase("coordinator")){
                                data.add("NOT_SENT");
                                commitData.add("NOT_SENT");
                            }
                            break;
                        }
                    }
                }

                /* Now that a successful name has been chosen, add the
                   socket's print writer to the set of all writers so
                   this client can receive broadcast messages. */
                out.println("NAMEACCEPTED");
                writers.add(out);


                //display names of all the clients added to the chat
                messageArea.append("\n" + name + " added to the chat");

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {

                    String input = in.readLine();
                    // Split the messages from the client into parts for HTTP processing
                    String[] httpParts = input.split(" ");
                    //process the original message to filter the HTTP encoding
                    input = input.replace("POST /localhost/serverPost HTTP/1.1 User-Agent: Host: content-type: text/plain; charset=utf-8 Content-Length: Date: Connection:Keep-Alive", "");
                        // get the system date to display it along with the HTTP request
                        DateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
                        // get the actual message length without the HTTP encoding
                        int msgLen = input.length() - 12;
                        Date d = new Date();
                        String date = sdf1.format(d);
                        /* Process the previously split message to build the HTTP request that came along with
                           all the information */
                        newString = httpParts[0] + " " + httpParts[1] + " " + httpParts[2] + "\n" + httpParts[3] + "SHARMA_Chatroom \n" + httpParts[4] + name + "\n" + httpParts[5] + httpParts[6] + " "
                                + httpParts[7] + "\n" + httpParts[8] + " " + msgLen + "\n" + httpParts[9] + " " + date + "\n" + httpParts[10];
                        messageArea.append("\n" + name + ":" + input);
                        messageArea.append("\n" + newString);
                        if(!(input.contains("COMMIT") || input.contains("ABORT") || input.contains("VOTE"))){
                            tempData.add(name + ": " + input);
                        }

                        if (input == null) {
                            return;
                        }
                        
                        for (PrintWriter writer : writers) {
                            
                        	/* Check whether if Pre-Commit Phase
                        	 * already exists.
                             */
                        		for (int j=0; j<data.size(); j++){
                                if(data.get(j).equalsIgnoreCase("PRE_COMMIT")){
                                	abortFlag = true;
                                }
                                else{
                                	abortFlag =false;
                                }
                        	}
                                	
                        		/* Check if any of the client votes to Abort
                        		 * and if someone does, send a Global-Abort 
                        		 * to the Server.
                        		 */   	
                            if (httpParts[12].equalsIgnoreCase("ABORT")){
                            	/*
                            	 * If pre-commit phase is complete
                            	 * the clients cannot abort at that stage.
                            	 */
                            	if(abortFlag){
                            		writer.println("MESSAGE " + name + " cannot abot at this time");
                            	}
                            	else{
                            	writer.println("MESSAGE " + name + " aboted");
                                writer.println("MESSAGE GLOBAL_ABORT" );
                                    for (int j=0; j<data.size() - 1; j++){
                                        data.set(j,"NOT_SENT");
                                    }
                            	}
                            }
                            
                            /* Check on whether the clients all Pre-Commit
	                            and if they do, send Pre-Commit phase complete
	                            message to the Server.
                             */
                            else if (httpParts[12].equalsIgnoreCase("PRE_COMMIT")){
                            	if (!inputFromAll){
                                writer.println("MESSAGE " + name + " ACKNOWLEDGED");
                                data.set(handlers.indexOf(this), "PRE_COMMIT");
                                for (int i=0; i<data.size(); i++){
                                	/*
                                	 * If all the clients agreed on pre-commit
                                	 * set the Pre-Commit flag (inputFromAll)
                                	 * to true else tell the clients that still waiting 
                                	 * for the remaining votes.
                                	 */
                                    if(!data.get(i).equalsIgnoreCase("NOT_SENT")){
                                        inputFromAll = true;
                                    }
                                    else {
                                        inputFromAll = false;
                                        writer.println("MESSAGE " + "Waiting for the response from other clients....");
                                        break;
                                    }
                                }
                            	}
                            	/*
                            	 * if the pre-commit flag is set to true
                            	 * send Pre-Commit phase complete message.
                            	 */
                                if (inputFromAll){
                                    writer.println("MESSAGE" + "PRE_COMMIT Phase Complete....");
                                    }
                             }
                            
                             /*Check on whether the clients all Global- Commit
	                            and if they do, send Global-Commit phase complete
	                            message to the Server.
	                         */
                            else if(httpParts[12].equalsIgnoreCase("COMMIT")){
                            	 writer.println("MESSAGE " + name + " COMMITTED");
                                 commitData.set(handlers.indexOf(this), "COMMIT");
                                 for (int i=0; i<data.size(); i++){
                                	 /*
                                	  * If all clients agree to Commit finally,
                                	  * set the Commit flag (commitFlag) to true 
                                	  * else tell the clients that still waiting 
                                	  * for the remaining votes.
                                	  */
                                     if(!commitData.get(i).equalsIgnoreCase("NOT_SENT")){
                                         commitFlag = true;
                                     }
                                     else {
                                         commitFlag = false;
                                         writer.println("MESSAGE " + "Waiting for the response from other clients....");
                                         break;
                                     }
                                 }
                                 
                                 /*
                                  * if the Commit flag is set to true
                                  * send the Global Commit Message
                                  */
                                 if (commitFlag){
                                     writer.println("MESSAGE" + "GLOBAL_COMMIT");
                                     }
                             }
                            
                            /*
                             * If a regular message is received
                             * relay it to all the clients.
                             */
                            else {
                            writer.println("MESSAGE " + name + " : " + input);}
                        }
                        
                        if (commitFlag){
                            for (int j=0; j<tempData.size(); j++){
                                fileWriter.println(tempData.get(j));
                            }
                        }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down! Remove its name and the printwriter from the sets, and close the socket socket.
                if (name != null) {
                    names.remove(name);
                    messageArea.append("\n" + name + " left the chat!! ");
                }
                if (out != null) {
                	//If the client exits, remove the corresponding printwriter from the list.
                    writers.remove(out);
                    for (PrintWriter writer: writers){
                        writer.println("MESSAGE " + name + " left the chat!!");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public HashSet<String> getList(){
        return names;
    }
}
