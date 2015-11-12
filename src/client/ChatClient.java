package client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * ChatClient.java
 * 
 * Version 1.0
 * Autor: Marc Kaepke & Anna Steinhauer
 * Zweck: Ein TCP-Client verbindet sich, mit anderen Clients, mit einem Chat-Server.
 * 		  Der Client folgt dabei dem Chat-Protokoll (SUBMITNAME, NAMEACCEPTED, MESSAGE und QUIT). 	
 * 		  Der Client verfuegt ueber eine grafische Swing-Oberflaeche und zeigt den Chat-Verlauf an.
 */
public class ChatClient {
	
	private BufferedReader in;	// Eingabestream vom Server
	private PrintWriter out;	// Ausgabestream zum Server
	
	private JFrame frame = new JFrame("HAW - RN");	// Chat-Fenster
	private JTextField textField = new JTextField(40);	// Eingabezeile 
	private JTextArea messageArea = new JTextArea(8, 40);	// Ausgabebereich von allen Chat-Mitgliedern
	private JScrollPane scrollPane = new JScrollPane(messageArea);
	
	private String nickname = "anonym";	// Initial Nickname
	
	private String hostname;	// Hostname
	private final int PORT = 56789;	// Portnummer, als Konstante
	
	/**
	 * Konstruktor
	 */
	public ChatClient() {
		builtGUI();
	}
	
	/**
	 * Erzeugt die GUI eines Clients und setzt Listeners.
	 */
	private void builtGUI() {
		// Layout GUI
		textField.setEditable(false);	// wird erst nach erfolgreichem Anmelden am Chat-Server aktiviert
		messageArea.setEditable(false);	// Nachrichten-Bereicha kann nicht veraendert werden
		frame.getContentPane().add(textField, BorderLayout.SOUTH);
		frame.getContentPane().add(scrollPane, "Center");
		frame.pack();
		
		// Listener auf das Beenden/ Schliessen des Fensters 
		frame.addWindowListener(new WindowAdapter() {
			/**
			 * Sobald das Fenster geschlossen wird, ohne dass der Benutzer sich abgemeldet hat,
			 * wird der Chat-Server ueber den Befehl "/quit" informiert, dass der Benutzer den Chat verlassen hat.
			 */
			@Override
			public void windowClosing(WindowEvent e) {
				out.println("/QUIT");
			}
			@Override
			public void windowClosed(WindowEvent e) {
				out.println("/QUIT");
			}
		});
				
		// Listener auf der Eingabezeile
		textField.addActionListener(new ActionListener() {
			/**
			 * Sobald die ENTER-Taste gedrueckt wurde, wird der Inhalt an den Chat-Server geschickt.
			 * Anschließend wird die Eingabezeile entleert/ geloescht
			 */
			
			// TODO neuen Thread starten
			public void actionPerformed(ActionEvent e) {
				out.println(textField.getText());
				textField.setText("");
			}
		});
	}
	
	/**
	 * Erzeugt ein InputDialog und fordert zur Eingabe des Hostnames auf.
	 * 
	 * @return Hostname des zu verbindenen Servers
	 */
	private String getServerAddress() {
		do {
			hostname = JOptionPane.showInputDialog(frame, "IP Adresse vom Chat-Server:", "RNP2", JOptionPane.QUESTION_MESSAGE);
		} while(hostname.isEmpty());	// wiederholt bis Hostname gefuellt ist
		return hostname;
	}
	
	/**
	 * Erzeugt ein InputDialog und fordert zur Eingabe des Nicknames auf.
	 * 
	 * @return Nickname des Benutzers
	 */
	private String getName() {
		do {
			nickname = JOptionPane.showInputDialog(frame, "Nickname angeben:", "Nickname", JOptionPane.PLAIN_MESSAGE);
		} while(nickname.isEmpty());	// wiederholt bis Nickname gefuellt ist
		return nickname;
	}
	
	/**
	 * Verbindet sich mit dem Chat-Server und tritt der Prozess-Schleife bei.
	 */
	private void startClient() throws IOException {
		
		/**
		 * Erzeugt den TCP-Socket und verbindet sich.
		 * Wenn Hostname ungueltig/ nicht erreichbar, dann wiederhole Eingabe.
		 */
		String serverAddress = null;
		Socket socket = null;
		boolean isConnected = false;
		do {
			try {
				serverAddress = getServerAddress();
				socket = new Socket(serverAddress, PORT);	// Verbindungsaufbau zum Server
				isConnected = true;
			} catch(NoRouteToHostException e) {
				System.err.println(e);
				isConnected = false;
			} catch(UnknownHostException e2) {
				System.err.println(e2);
				isConnected = false;
			} catch(ConnectException e3) {
				System.err.println(e3);
				isConnected = false;
			}
		} while(!isConnected);

		// Initialisiere In- und Out-Stream
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		
		// Client beenden?
		boolean serviceRequested = true;
		
		/**
		 * Durchlaeuft das Protokoll bis Client beendet wird.
		 * 
		 * SUBMITNAME -> Aufforderung fuer Nickname
		 * NAMEACCEPTED -> Nickname wird beim Server registriert
		 * MESSAGE -> Es folgt eine Chat-Nachricht vom Server
		 * QUIT -> Beendet den Client und meldet ihn ab
		 */
		// Anhand der Befehle "
		while(serviceRequested) {
			try {
				String line = in.readLine();	// Antwort vom Server
				
				// Aufforderung fuer Angabe des Nicknames; serverseitig
				if(line.startsWith("SUBMITNAME")) {
					out.println(getName());
					frame.setTitle(frame.getTitle() + " as " + nickname);	// passte Titel vom Client Fenster an
					
				// Nickname wurde vom Server akzeptiert -> Eingabefeld wird aktiviert
				} else if(line.startsWith("NAMEACCEPTED")) {
					textField.setEditable(true);
					textField.requestFocusInWindow();
				
				// Server schickt Chat-Nachricht an Client zum Anzeigen im Ausgabebereich 
				} else if(line.startsWith("MESSAGE")) {
					messageArea.append(line.substring(8) + "\n");
					scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
					
				// Server schickt Bestaetigung; Client erfolgreich abgemeldet
				} else if(line.startsWith("QUIT")) {
					serviceRequested = false;
					JOptionPane.showMessageDialog(frame, "You left the conversation.", "Good bye", JOptionPane.OK_OPTION);
					frame.dispose();
				}
				
			// Fehlermeldung, wenn der Chat-Server nicht mehr erreichbar ist
			} catch(NullPointerException e) {
				serviceRequested = false;
				JOptionPane.showMessageDialog(frame, "Der Chat-Server ist nicht mehr erreichbar.", "Achtung", JOptionPane.OK_OPTION);
				frame.dispose();
			}
		}
		socket.close(); // Verbindungsabbau
	}
	
	/**
	 * Startet einen Client 
	 */
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setLocationRelativeTo(null); // positioniert das Fenster mittig
		client.frame.setVisible(true);	// macht es sichtbar
		client.startClient();	// startet den Client
	}
}
