package bgu.spl.net.impl.tftp;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ListeningThread implements Runnable {
    private final InputStream in; // InputStream to read data from the network
    private final OutputStream out; // OutputStream to send data to the network
    private CommandState commandState;
    String pathtofiles = "." + File.separator;
    FileTransferState filet;



    public ListeningThread(InputStream in, OutputStream out,CommandState commandState) {
        this.in = in;
        this.out = out;
        this.commandState = commandState;
        filet=new FileTransferState();

    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024]; // Buffer for reading data
            while (true) {
                int length = in.read(buffer); // Read data into buffer
                if (length == -1) {
                    break;
                }

                handlePacket(buffer, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePacket(byte[] packet, int length) {
        TftpEncoderDecoder decoder = new TftpEncoderDecoder();
        int opCode = -1;
        for (int i = 0; i < length; i++) {
            byte[] message = decoder.decodeNextByte(packet[i]);
            if (!decoder.havinopcode())
                opCode = decoder.getopcode();
            if (message != null) {
                processDecodedMessage(message, opCode);
            }
        }
    }

    private void processDecodedMessage(byte[] message, int opcode) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        switch (opcode) {
            case 3: // DATA packet
                int blockNumber = buffer.getShort(2);
                byte[] data = new byte[buffer.limit() - 4];
                buffer.position(4);
                buffer.get(data, 0, data.length);
                // System.out.println("DATA packet received, block number: " + blockNumber);

                // If in DIRQ mode, concatenate file listing data
                if (commandState.isDirqActive()) {
                    commandState.appendToDirList(data);

                    // If last DATA packet, print file list
                    if (data.length < 512) {
                        System.out.println(commandState.getDirList());
                        commandState.setDirqActive(false); // Reset DIRQ flag
                        commandState.resetDirList();
                    }
                } else {
                    // Handle saving file data for RRQ
                    String filename = commandState.getFilename();
                    saveDataToFile(filename, data);
                }

                sendAck(blockNumber);
                break;
            case 4: // ACK packet
                int ackBlockNumber = buffer.getShort(0); // Assuming ACK format follows opcode (2 bytes) + block number (2 bytes)
                System.out.println("ACK " + ackBlockNumber);
                if (commandState.isTransferFile()) {
                    // Successfully received ACK for the last block sent, prepare to send next block
                    if(ackBlockNumber == 0)
                        processWRQ();
                    else
                        processWRQ(filet.getCurrentBlockNumber());
                }

                if(ackBlockNumber == 0 && commandState.isDisconnecting()) { // Assuming isDisconnecting() indicates waiting for DISC ACK
                    try {
                        out.close(); // Close the output stream
                        in.close(); // Close the input stream
                        System.exit(0); // Exit the program
                    } catch (Exception e) {
                        e.printStackTrace(); // Print any exception during closure
                    }
                }
                break;
            case 5: // ERROR packet
                int errorCode = buffer.getShort(0); // Assuming ERROR format follows opcode (2 bytes) + error code (2 bytes)
                String errorMessage = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);
                System.out.println("ERROR " + errorCode + " " + errorMessage);
                break;
            case 9: // BCAST packet
                boolean isAdded = buffer.get(0) == 1; // Assuming BCAST format follows opcode (2 bytes) + add/delete flag (1 byte)
                String fileName = new String(message, 1, message.length - 1, StandardCharsets.UTF_8);
                System.out.println("BCAST " + fileName + " " + (isAdded ? "add" : "del"));
                break;
            // Other cases...
        }
    }

    private void sendAck(int blockNumber) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) 0); // 0
            buffer.put((byte) 4); // ACK opcode
            buffer.putShort((short) blockNumber); // Block number

            out.write(buffer.array());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDataToFile(String filename, byte[] data) {
        try (FileOutputStream fos = new FileOutputStream(new File(filename), true)) {
            fos.write(data);
        } catch (Exception e) {
            System.err.println("Error saving data to file: " + e.getMessage());
        }
    }

    private byte[] createDataPacket(short blockNumber, byte[] data, int length) {
        // Adjusting the packet size to accommodate the message size field
        byte[] dataPacket = new byte[length + 6];
        dataPacket[0] = 0;
        dataPacket[1] = 3; // Opcode for DATA

        // Assuming the message size is the length of the data
        dataPacket[2] = (byte)(length >> 8);
        dataPacket[3] = (byte)(length);

        dataPacket[4] = (byte)(blockNumber >> 8);
        dataPacket[5] = (byte)(blockNumber);

        System.arraycopy(data, 0, dataPacket, 6, length);
        return dataPacket;
    }


    public void processWRQ( ) {
        File file = new File(pathtofiles, commandState.getFilename());



        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
//            this.filet.setRrqpacket(rrqPacket);
            byte[] buffer = new byte[512];
            int bytesRead;


            if ((bytesRead = raf.read(buffer)) != -1) {
                byte[] dataPacket = createDataPacket((short) 1, buffer, bytesRead);
                this.filet.setCurrentupload(true);
                this.filet.setCurrentBlockNumber(1);
                out.write(dataPacket);
                this.filet.setCurrentBlockNumber(2);


                if (bytesRead < 512){
//                    this.filet.setRrqpacket(null);
                    this.filet.setCurrentupload(false);
                    this.filet.setCurrentBlockNumber(0);
                    System.out.println("WRQ " + commandState.getFilename() +" complete");
                    commandState.setTransferFile(false);

                }
            }
        } catch (Exception e) {
        }
    }








    public void processWRQ(  int blockNumber) {
        File file = new File(pathtofiles, commandState.getFilename());





        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer = new byte[512];
            int bytesRead;
            long filePointer = (blockNumber-1) * 512; // Calculate the starting point
            raf.seek(filePointer); // Set the file pointer to the start of the block

            if ((bytesRead = raf.read(buffer)) != -1) {

                byte[] dataPacket = createDataPacket((short) blockNumber, buffer, bytesRead);

                out.write(dataPacket);


                this.filet.setCurrentBlockNumber(blockNumber+1);
                if (bytesRead < 512){
//                    this.filet.setRrqpacket(null);
                    this.filet.setCurrentupload(false);
                    this.filet.setCurrentBlockNumber(0);
                    System.out.println("WRQ " + commandState.getFilename() +" complete");
                    commandState.setTransferFile(false);
                }
            }
            else{
//                this.filet.setRrqpacket(null);
                this.filet.setCurrentupload(false);
                this.filet.setCurrentBlockNumber(0);
                System.out.println("WRQ " + commandState.getFilename() +" complete");
                commandState.setTransferFile(false);
            }
        } catch (Exception e) {
        }
    }


}
