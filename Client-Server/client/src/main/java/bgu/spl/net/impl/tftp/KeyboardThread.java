package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.OutputStream;
import java.util.Scanner;

public class KeyboardThread implements Runnable {
    private final OutputStream out; // Network output stream to send commands to the server.
    private CommandState commandState;

    public KeyboardThread(OutputStream out, CommandState commandState) {
        this.out = out; // Constructor initializes the output stream.
        this.commandState = commandState;

    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in); // Scanner to read user input from console.
        while (true) { // Infinite loop to continuously read user input.
            String input = scanner.nextLine(); // Read next line of input from the user.
            try {
                byte[] packet = handleCommand(input); // Process the input command and get the packet.
                if (packet != null) {
                    out.write(packet); // Send the packet to the server if it's not null.
                }
            } catch (Exception e) {
                e.printStackTrace(); // Print any exceptions to standard error and exit the loop.
                break;
            }
        }
    }

    private byte[] handleCommand(String command) {
        // Split the input command into parts to extract the opcode and any arguments.
        String[] parts = command.split(" ", 2);
        String opcode = parts[0].toUpperCase(); // Command name, converted to uppercase.
        String argument = parts.length > 1 ? parts[1] : ""; // The argument of the command, if any.
        // If the command is RRQ or WRQ, save the filename to CommandState.
        // Handle RRQ and WRQ commands.
        if ("RRQ".equals(opcode) || "WRQ".equals(opcode)) {
            File file = new File(argument);
            if (file.exists() && "RRQ".equals(opcode) && commandState.isLogin()) {
                System.out.println("File already exists");
                return null;
            }// Don't send RRQ packet to the server
            if (!file.exists() && "WRQ".equals(opcode) && commandState.isLogin()) {
                System.out.println("File does not exist");
                return null;
            } // Don't send WRQ packet to the server
            else
                commandState.setFilename(argument); // Save the filename to CommandState
        }


        // Switch statement to handle different commands and generate the appropriate packet.
        switch (opcode) {
            case "LOGRQ":
                commandState.setLogin(true);
                return TftpPacketCreator.createLogRqPacket(argument); // Handle login request.
            case "DELRQ":
                return TftpPacketCreator.createDelRqPacket(argument); // Handle delete request.
            case "RRQ":
                return TftpPacketCreator.createRrqPacket(argument); // Handle read request.
            case "WRQ":
                if (commandState.isLogin()) {
                    commandState.setTransferFile(true);
                }
                return TftpPacketCreator.createWrqPacket(argument); // Handle write request.
            case "DIRQ":
                if (commandState.isLogin()) {
                    commandState.setDirqActive(true);
                }
                return TftpPacketCreator.createDirqPacket(); // Handle directory listing request.
            case "DISC":
                if (commandState.isLogin()) {
                    commandState.setDisconnecting(true);
                }
                return TftpPacketCreator.createDiscPacket(); // Handle disconnect request.
            default:
                System.out.println("Unknown command."); // Print error message for unrecognized commands.
                return null; // Return null for unrecognized commands.
        }
    }
}

