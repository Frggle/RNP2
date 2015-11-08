package server;

/*
 * TCPServer.java
 *
 * Version 3.1
 * Autor: M. Huebner HAW Hamburg (nach Kurose/Ross)
 * Zweck: TCP-Server Beispielcode:
 *        Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine Anfrage bearbeitet:
 *        einen String empfangen, in Grossbuchstaben konvertieren und zuruecksenden
 *        Maximale Anzahl Worker-Threads begrenzt durch Semaphore
 *  
 */
import java.io.*;
import java.net.*;
import java.util.concurrent.*;


public class ChatServer {
   /* TCP-Server, der Verbindungsanfragen entgegennimmt */

   /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads  */
   public Semaphore workerThreadsSem;

   /* Portnummer */
   public final int serverPort;
   
   /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
   public boolean serviceRequested = true;
		 
   /* Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler Worker-Threads*/
   public ChatServer(int serverPort, int maxThreads) {
      this.serverPort = serverPort;
      this.workerThreadsSem = new Semaphore(maxThreads);
   }

   public void startServer() {
      ServerSocket welcomeSocket; // TCP-Server-Socketklasse
      Socket connectionSocket; // TCP-Standard-Socketklasse

      int nextThreadNumber = 0;

      try {
         /* Server-Socket erzeugen */
         welcomeSocket = new ServerSocket(serverPort);

         while (serviceRequested) { 
				workerThreadsSem.acquire();  // Blockieren, wenn max. Anzahl Worker-Threads erreicht
				
            System.out.println("TCP Server is waiting for connection - listening TCP port " + serverPort);
            /*
             * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
             * Standard-Socket erzeugen und an connectionSocket zuweisen
             */
            connectionSocket = welcomeSocket.accept();

            /* Neuen Arbeits-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt uebergeben */
            (new TCPWorkerThread(++nextThreadNumber, connectionSocket, this)).start();
          }
      } catch (Exception e) {
         System.err.println(e.toString());
      }
   }

   public static void main(String[] args) {
      /* Erzeuge Server und starte ihn */
      ChatServer myServer = new ChatServer(56789, 1);
      myServer.startServer();
   }
}

// ----------------------------------------------------------------------------

class TCPWorkerThread extends Thread {
   /*
    * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
    * erhaelt
    */
   private int name;
   private Socket socket;
   private ChatServer server;
   private BufferedReader inFromClient;
   private DataOutputStream outToClient;
   boolean workerServiceRequested = true; // Arbeitsthread beenden?

   public TCPWorkerThread(int num, Socket sock, ChatServer server) {
      /* Konstruktor */
      this.name = num;
      this.socket = sock;
      this.server = server;
   }

   public void run() {
      String capitalizedSentence;

      System.out.println("TCP Worker Thread " + name +
            " is running until QUIT is received!");

      try {
         /* Socket-Basisstreams durch spezielle Streams filtern */
         inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         outToClient = new DataOutputStream(socket.getOutputStream());

         while (workerServiceRequested) {
            /* String vom Client empfangen und in Grossbuchstaben umwandeln */
            capitalizedSentence = readFromClient().toUpperCase();

            /* Modifizierten String an Client senden */
            writeToClient(capitalizedSentence);

            /* Test, ob Arbeitsthread beendet werden soll */
            if (capitalizedSentence.startsWith("QUIT")) {
               workerServiceRequested = false;
            }
         }

         /* Socket-Streams schliessen --> Verbindungsabbau */
         socket.close();
      } catch (IOException e) {
         System.err.println("Connection aborted by client!");
      } finally {
         System.out.println("TCP Worker Thread " + name + " stopped!");
         /* Platz fuer neuen Thread freigeben */
			server.workerThreadsSem.release();         
      }
   }

   private String readFromClient() throws IOException {
      /* Lies die naechste Anfrage-Zeile (request) vom Client */
      String request = inFromClient.readLine();
      System.out.println("TCP Worker Thread " + name + " detected job: " + request);

      return request;
   }

   private void writeToClient(String reply) throws IOException {
      /* Sende den String als Antwortzeile (mit CRLF) zum Client */
      outToClient.writeBytes(reply + '\r' + '\n');
      System.out.println("TCP Worker Thread " + name +
            " has written the message: " + reply);
   }
}
