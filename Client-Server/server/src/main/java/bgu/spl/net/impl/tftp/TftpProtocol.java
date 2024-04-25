package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.Connections;

import java.io.*;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


class holder{
    static ConcurrentHashMap<Integer, Boolean> ids_login = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Boolean> files = new ConcurrentHashMap<>();
}



public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {


    String pathtofiles = "Files";
    private boolean shouldTerminate = false;

    private int connectionId;
    private Connections<byte[]> connections;

    FileTransferState filet;



    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
       this.shouldTerminate = false;
       this.connectionId = connectionId;
       this.connections = connections;
       holder.ids_login.put(connectionId,true);
       filet = new FileTransferState(connectionId);

    }

    @Override
    public void process(byte[] message,int opcode, BlockingConnectionHandler<byte[]> handler) {
        boolean exist = connections.existalready(connectionId);
        if(opcode == 7){
            String name = new String(message, 0, message.length-1, StandardCharsets.UTF_8).trim();
            if(!exist && !connections.nameexist(connectionId)){
                connections.connect(connectionId,handler);
                connections.connectname(connectionId,name);
                sendAck(connectionId,(short)0);
            }
            else{
                sendError(connectionId,(short)7,"User already logged in – Login username already connected.");
            }

        }

        else if(!exist){
                connections.connect(connectionId,handler);
                sendError(connectionId,(short)6,"User not logged in – Any opcode received before Login completes");
                connections.disconnect(connectionId);
                holder.ids_login.remove(connectionId);
            }
        else{
            if(opcode == 8){
                processDelRqPacket(connectionId,message);
            }
            if(opcode == 1){
                processRRQ(connectionId,message);
            }
            if(opcode == 4){
                receiveAck(message);
            }
            if (opcode == 2){
                processWRQ(connectionId,message);
            }
            if(opcode == 3){
                receiveData(connectionId,message);

            }
            if(opcode == 6){
                processDIRQ(connectionId);

            }
            if(opcode == 10){
                processDISC(connectionId);

            }





        }



    }
    public void processDISC(int connectionId) {
        // Send ACK in response to DISC
        sendAck(connectionId, (short) 0);   
        disconnectClient(connectionId);

        // Perform any necessary cleanup or resource release for this client




    }

    private void disconnectClient(int connectionId) {

        connections.disconnect(connectionId);
        holder.ids_login.remove(connectionId);
        connections.disconnectname(connectionId);

        // Log or print the disconnection
        System.out.println("Client " + connectionId + " disconnected.");
    }

    public void sendBCAST(byte[] filename,byte deloradd) {
        byte[] bcast = new byte[filename.length+4];
        bcast[0] = 0;
        bcast[1] = 9;
        bcast[2] = deloradd;
        bcast[bcast.length-1] = 0;
        System.arraycopy(filename, 0, bcast, 3, filename.length);
        List<Integer> sum = connections.getallconnections();
        for(Integer k : sum){
            this.connections.send(k,bcast);
        }



    }





















    public void sendAck(int connectionId, short blockNumber) {
        byte[] ackPacket = new byte[4]; // ACK packet size is 4 bytes (2 for opcode, 2 for block number)

        // Opcode for ACK is 4
        ackPacket[0] = 0;
        ackPacket[1] = 4;

        // Setting the block number
        ackPacket[2] = (byte)(blockNumber >> 8);// Extracts the MSB
        ackPacket[3] = (byte)(blockNumber);

        connections.send(connectionId, ackPacket);
    }


    public void sendError(int connectionId, short errorCode, String errorMessage) {
        // Convert the error message to UTF-8 bytes and add the zero byte at the end
        byte[] errorMessageBytes = (errorMessage + "\0").getBytes(StandardCharsets.UTF_8);

        // Calculate the total length of the packet
        int packetLength = 2 + 2 + errorMessageBytes.length; // Opcode (2 bytes) + Error Code (2 bytes) + Error Message

        // Create the packet byte array
        byte[] errorPacket = new byte[packetLength];

        // Opcode for ERROR is 5
        errorPacket[0] = 0;
        errorPacket[1] = 5;

        // Setting the error code
        errorPacket[2] = (byte)(errorCode >> 8);
        errorPacket[3] = (byte)(errorCode);

        // Copy the error message into the packet
        System.arraycopy(errorMessageBytes, 0, errorPacket, 4, errorMessageBytes.length);

        // Send the packet
        connections.send(connectionId, errorPacket);
    }



    public void processDelRqPacket(int connectionId, byte[] delRqPacket) {
        // Extract the file name from the DELRQ packet
        String fileName = new String(delRqPacket, 0, delRqPacket.length-1, StandardCharsets.UTF_8).trim();

        File file = new File(pathtofiles, fileName);

        try {
            if (file.exists()) {
                // File exists, proceed with deletion
                boolean deleted = file.delete();
                if (deleted) {
                    // Send ACK if file successfully deleted
                    sendAck(connectionId, (short) 0);
                    byte[] newArray = Arrays.copyOfRange(delRqPacket, 0, delRqPacket.length-1);
                    sendBCAST(newArray, (byte) 0);


                } else {
                    // Handle failure to delete
                    sendError(connectionId, (short) 1, "Failed to delete file.");
                }
            } else {
                // File does not exist, send error
                sendError(connectionId, (short) 1, "File not found.");
            }
        } catch (Exception e) {
            // Handle any exceptions, possibly sending an error packet
            sendError(connectionId, (short) 1, "Error processing request.");
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


    public void processRRQ(int connectionId, byte[] rrqPacket) {
        String filename = new String(rrqPacket, 0, rrqPacket.length - 1, StandardCharsets.UTF_8).trim();
        File file = new File(pathtofiles, filename);



        if (!file.exists()) {
            sendError(connectionId, (short) 1, "File not found.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            this.filet.setRrqpacket(rrqPacket);
            byte[] buffer = new byte[512];
            int bytesRead;


            if ((bytesRead = raf.read(buffer)) != -1) {
                byte[] dataPacket = createDataPacket((short) 1, buffer, bytesRead);
                this.filet.setCurrentupload(true);
                this.filet.setCurrentBlockNumber(1);
                connections.send(connectionId, dataPacket);
                this.filet.setCurrentBlockNumber(2);


                if (bytesRead < 512){
                    this.filet.setRrqpacket(null);
                    this.filet.setCurrentupload(false);
                    this.filet.setCurrentBlockNumber(0);
                    System.out.println("RRQ" + filename +"complete");

                }
            }
        } catch (Exception e) {
            sendError(connectionId, (short) 1, "Error reading file.");
        }
    }








    public void processRRQ(int connectionId, byte[] rrqPacket, int blockNumber) {
        String filename = new String(rrqPacket, 0, rrqPacket.length - 1, StandardCharsets.UTF_8).trim();
        File file = new File(pathtofiles, filename);



        if (!file.exists()) {
            sendError(connectionId, (short) 1, "File not found.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer = new byte[512];
            int bytesRead;
            long filePointer = (blockNumber-1) * 512; // Calculate the starting point
            raf.seek(filePointer); // Set the file pointer to the start of the block

            if ((bytesRead = raf.read(buffer)) != -1) {
                byte[] dataPacket = createDataPacket((short) blockNumber, buffer, bytesRead);

                connections.send(connectionId, dataPacket);


                this.filet.setCurrentBlockNumber(blockNumber+1);
                if (bytesRead < 512){
                    this.filet.setRrqpacket(null);
                    this.filet.setCurrentupload(false);
                    this.filet.setCurrentBlockNumber(0);
                    System.out.println("RRQ" + filename +"complete");
                }
            }
        } catch (Exception e) {
            sendError(connectionId, (short) 1, "Error reading file.");
        }
    }


    public void receiveAck(byte[] ackPacket) {
        // Assuming the first two bytes are the opcode and the next two bytes are the block number.
        short blockNumber = (short) ((ackPacket[0] << 8& 0XFF) | (ackPacket[1] )& 0XFF);
        if(blockNumber == 0){
            System.out.println("ACK 0");
        }
        else{
            if(filet.isCurrentupload() == true){
                processRRQ(this.connectionId,filet.getRrqpacket(),filet.getCurrentBlockNumber());
            }
            if(filet.getDirqpacket()!=null){
                processDIRQ(this.connectionId);
            }

        }


    }




    public void processWRQ(int connectionId, byte[] wrqPacket) {
        // Extract the file name from the WRQ packet
        String filename = new String(wrqPacket, 0, wrqPacket.length - 1, StandardCharsets.UTF_8).trim();
        File file = new File(pathtofiles, filename); // Adjust the folder name as needed

        try {
            if (!file.exists()) {
                // File does not exist, create a new file
                boolean created = file.createNewFile();
                if (created) {
                    this.filet.setWrqpacket(wrqPacket);
                    this.filet.setCurrentdownload(true);
                    this.filet.setCurrentBlockNumber(0);
                    holder.files.put(filename,true);
                    // File created successfully, send ACK with block number 0
                    sendAck(connectionId, (short) 0);
                } else {
                    // Failed to create the file, send an appropriate error
                    sendError(connectionId, (short) 1, "Failed to create file.");
                }
            } else {
                // File already exists, send error
                sendError(connectionId, (short) 5, "File already exists.");
            }
        } catch (Exception e) {
            // Handle any exceptions, possibly sending an error packet
            sendError(connectionId, (short) 1, "Error processing request.");
        }
    }




    public void receiveData(int connectionId, byte[] dataPacket) {
        System.out.println(Arrays.toString(dataPacket) +"\n" + "handling data");
        // Extract the block number and data from the dataPacket
        short blockNumber = (short) (((short) dataPacket[2]& 0XFF) << 8 | (short) (dataPacket[3])& 0XFF);
        short size = (short) (((short) dataPacket[0] & 0XFF) << 8 | (short) (dataPacket[1] & 0XFF));
        byte[] data = Arrays.copyOfRange(dataPacket, 4, size);


        String filename = new String(this.filet.getWrqpacket(), 0, this.filet.getWrqpacket().length - 1, StandardCharsets.UTF_8).trim();
        File file = new File(pathtofiles, filename); // Adjust the folder name as needed
        long filePointer = (blockNumber - 1) * 512; // Calculate the starting point

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(filePointer); // Set the file pointer to the start of the block
            raf.write(data); // Write the data
            if(size<512){
                this.filet.setCurrentBlockNumber(0);
                this.filet.setCurrentdownload(false);
                ;
                System.out.println("WRQ" +filename +"complete");
                holder.files.remove(filename);
                sendAck(connectionId, blockNumber);
                byte[] tocast = Arrays.copyOfRange(filet.getWrqpacket(), 0, filet.getWrqpacket().length-2);
                sendBCAST(tocast, (byte) 1);
                this.filet.setWrqpacket(null);
            }
            else{
                sendAck(connectionId, blockNumber);
                this.filet.setCurrentBlockNumber(blockNumber+1);
            }



            System.out.println("send block "+ blockNumber);
        } catch (IOException e) {
            // Handle any exceptions, possibly sending an error packet
            sendError(connectionId, (short) 1, "Error writing file.");
        }
    }




    public void processDIRQ(int connectionId) {
        File directory = new File(pathtofiles); // Directory containing the files
        File[] files = directory.listFiles();

        // Create a string with filenames separated by a zero byte
        StringBuilder sb = new StringBuilder();
        if (files != null) {

            for (File file : files) {
                if (holder.files.get(file.getName())==null) {
                    sb.append(file.getName()).append("\0");}

            }
        }

        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        filet.setDirqpacket(data);
        // Split the data into chunks of 512 bytes and send each as a DATA packet
        this.filet.setCurrentBlockNumber(1);

            byte[] chunk = Arrays.copyOfRange(data, filet.getCurrentBlockNumber()-1, Math.min(data.length, (filet.getCurrentBlockNumber()-1) + 512));
            byte[] dataPacket = createDataPacket((short) filet.getCurrentBlockNumber(), chunk, chunk.length);
            connections.send(connectionId, dataPacket);
        if(chunk.length<512) {
            this.filet.setCurrentBlockNumber(0);
            this.filet.setCurrentdownload(false);
            this.filet.setDirqpacket(null);


        }else {
            filet.setCurrentBlockNumber(filet.getCurrentBlockNumber() + 1);
        }

        }












    @Override
    public boolean shouldTerminate() {
        return this.shouldTerminate;
    }



}



























