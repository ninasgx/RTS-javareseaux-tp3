import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * TCPClient for TP3
 *
 * - Connects to given host:port
 * - Reads user input from console
 * - Sends each non-empty line to server
 * - Prints server's echo
 * - Stops on /quit or when server closes connection
 */

public class TCPClient {

    private String host;
    private int port;

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        System.out.println("Connecting to server " + host + ":" + port + "...");

        try (
                Socket socket = new Socket(host, port);
                // Use system default encoding for console (on your Windows it's CP936/GBK)
                BufferedReader consoleReader = new BufferedReader(
                        new InputStreamReader(System.in));
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            System.out.println("Connected to server.");
            System.out.println("Type message and press Enter.");
            System.out.println("Type /quit to exit.");

            String userInput;

            while (true) {
                System.out.print("> ");
                userInput = consoleReader.readLine();

                if (userInput == null) {
                    System.out.println("Console input closed. Exiting client.");
                    break;
                }

                userInput = userInput.trim();
                if (userInput.isEmpty()) {
                    continue;
                }

                if ("/quit".equalsIgnoreCase(userInput)) {
                    System.out.println("Closing connection and exiting client.");
                    break;
                }

                // Send to server
                out.println(userInput);

                // Read echo from server
                String response = in.readLine();
                if (response == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }

                System.out.println("Server: " + response);
            }

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + host);
        } catch (ConnectException e) {
            System.err.println("Cannot connect to " + host + ":" + port + " - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error in client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TCPClient <host> <port>");
            return;
        }

        String host = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
            return;
        }

        TCPClient client = new TCPClient(host, port);
        client.start();
    }
}
