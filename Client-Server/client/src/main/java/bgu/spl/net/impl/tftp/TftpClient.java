package bgu.spl.net.impl.tftp;

import bgu.spl.net.impl.tftp.KeyboardThread;
import bgu.spl.net.impl.tftp.ListeningThread;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class TftpClient {
    public static void main(String[] args) {

        if (args.length == 0) {
            args = new String[]{"localhost", "7777"};
        }

        // Check for correct usage
        if (args.length != 2) {
            System.out.println("Usage: java TftpClient <server address> <port>");
            return;
        }

        String serverAddress = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(serverAddress, port)) {
            // Get input and output streams for the socket
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            CommandState commandState = new CommandState();

            // Create and start a thread for reading user input and sending commands
            Thread keyboardThread = new Thread(new KeyboardThread(outputStream,commandState));
            keyboardThread.start();

            // Create and start a thread for listening to server responses
            Thread listeningThread = new Thread(new ListeningThread(inputStream , outputStream,commandState));
            listeningThread.start();

            // Wait for both threads to finish
            keyboardThread.join();
            listeningThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
