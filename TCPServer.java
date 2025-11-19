import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.LinkedList;

/**
 * TCPServer (multi-client, TP3 version)
 *
 * - Listens on a TCP port
 * - Accepts multiple clients concurrently (one thread per client)
 * - For each client:
 *      * logs connect/disconnect with timestamp and client id
 *      * echoes each received line with prefix [client#id ip]
 * - Maintains a history of the last MAX_HISTORY messages (server-side only)
 */
public class TCPServer {

    /** Listening port */
    private int port;

    /** Server running flag */
    private boolean running = false;

    /** Global client id counter */
    private static int nextClientId = 0;

    /** History size */
    private static final int MAX_HISTORY = 10;

    /** Shared message history (last N messages) */
    private static final LinkedList<String> history = new LinkedList<>();

    public TCPServer() {
        this(8080);
    }

    public TCPServer(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "TCPServer{port=" + port + ", running=" + running + "}";
    }

    public void launch() {
        System.out.println("Starting TCPServer on port " + port + "...");
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            System.out.println("Server state: " + this.toString());

            while (running) {
                // Accept a new client
                Socket clientSocket = serverSocket.accept();

                int clientId;
                synchronized (TCPServer.class) {
                    clientId = ++nextClientId;
                }

                // Start a dedicated thread for this client
                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                handler.start();
            }

        } catch (IOException e) {
            System.err.println("Server I/O error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            running = false;
            System.out.println("Server stopped.");
        }
    }

    /**
     * Add a record to the global history (keep only last MAX_HISTORY).
     */
    private static void addToHistory(String record) {
        synchronized (history) {
            history.add(record);
            if (history.size() > MAX_HISTORY) {
                history.removeFirst();
            }
        }
    }

    /**
     * Thread that handles a single client.
     */
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final int clientId;

        ClientHandler(Socket clientSocket, int clientId) {
            this.clientSocket = clientSocket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();

            LocalDateTime connectTime = LocalDateTime.now();
            System.out.println(connectTime + " - client#" + clientId
                    + " connected from " + clientAddress + ":" + clientPort);

            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                    PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)
            ) {
                String line;
                String prefix = "[client#" + clientId + " " + clientAddress + "] ";

                while ((line = in.readLine()) != null) {
                    String record = prefix + line;

                    // Log on server
                    System.out.println(record);

                    // Update history
                    addToHistory(record);

                    // Echo back to THIS client
                    out.println(record);
                }

            } catch (IOException e) {
                System.err.println("I/O error with client#" + clientId + ": " + e.getMessage());
            } finally {
                LocalDateTime disconnectTime = LocalDateTime.now();
                System.out.println(disconnectTime + " - client#" + clientId
                        + " disconnected: " + clientAddress + ":" + clientPort);

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket for client#" + clientId + ": " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default 8080");
            }
        }

        TCPServer server = new TCPServer(port);
        server.launch();
    }
}
