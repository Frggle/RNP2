package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A multithreaded chat room server. When a client connects the server requests
 * a screen name by sending the client the text "SUBMITNAME", and keeps
 * requesting a name until a unique one is received. After a client submits a
 * unique name, the server acknowledges with "NAMEACCEPTED". Then all messages
 * from that client will be broadcast to all other clients that have submitted a
 * unique screen name. The broadcast messages are prefixed with "MESSAGE ".
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 56789;

    /**
     * The set of all names of clients in the chat room. Maintained so that we
     * can check that new clients are not registering name already in use.
     */
    private static Set<String> users = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients. This set is kept so
     * we can easily broadcast messages.
     */
    private static Set<PrintWriter> writers = new HashSet<PrintWriter>();

    /**
     * TODO
     */
    private static List<String> session = new ArrayList<String>();

    private static JFrame frame;
    private static JTextArea textArea;

    /**
     * The appplication main method, which just listens on a port and spawns
     * handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.print("The chat server is running");
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println(" on IP: "
                + InetAddress.getLocalHost().getHostAddress());

        frame = new JFrame("Chat Server - Log");
        textArea = new JTextArea(20, 40);
        textArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(textArea), "Center");
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        DateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        textArea.append("The Chat Server ist running");
        textArea.append(" on IP: "
                + InetAddress.getLocalHost().getHostAddress() + "\n");
        textArea.append("---" + dateformat.format(cal.getTime()) + "---\n");
        textArea.append("\n");

        Socket connectionSocket;
        try {
            while (true) {
                connectionSocket = listener.accept();
                new Handler(connectionSocket).start();
                // new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class. Handlers are spawned from the listening loop and
     * are responsible for a dealing with a single client and broadcasting its
     * messages.
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket. All the
         * interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a screen name
         * until a unique one has been submitted, then acknowledges the name and
         * registers the output stream for the client in a global set, then
         * repeatedly gets inputs and broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client. Keep requesting until
                // a name is submitted that is not already used. Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME"); // Bestaetigung vom Server an
                                               // Client
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    writeServerLog("        ", name + " SUBMITNAME");
                    synchronized (users) {
                        if (!users.contains(name)) {
                            users.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writeServerLog("        ", name + " NAMEACCEPTED");

                for(PrintWriter writer : writers) {
                	writer.println("MESSAGE        " + name + " joined");
                }
                writeServerLog("        ", name + " joined");
                writers.add(out);

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    } else if (input.toUpperCase().startsWith("/QUIT")) {
                        out.println("QUIT");
                        writeServerLog("        ", name + " disconnected");
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + " ("
                                    + sdf.format(cal.getTime()) + ") disconnected");
                        }
                    } else if (input.toUpperCase().startsWith("/USER")) {
                    	writeServerLog(name, input);
                        out.println("MESSAGE        list of users:");
                        for (String user : users) {
                            if (!user.equals(name)) {
                                out.println("MESSAGE        " + user);
                            }
                        }
                    } else if (input.toUpperCase().startsWith("/HELP")) {
                    	writeServerLog(name, input);
                        out.println("MESSAGE        /user => list of connected users.");
                        out.println("MESSAGE        /quit => disconnect from Chat-Server.");
                    } else {
                        for (PrintWriter writer : writers) {
                            writeServerLog(name, input);
                            writer.println("MESSAGE " + name + " ("
                                    + sdf.format(cal.getTime()) + ") : " + input);
                        }
                        
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down! Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    users.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        
        private void writeServerLog(String beforeDate, String afterDate) {
        	Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        	
        	session.add(beforeDate + " (" + sdf.format(cal.getTime()) + ") : " + afterDate);
        	textArea.append(session.get(session.size() - 1) + "\n");
        }
    }
}