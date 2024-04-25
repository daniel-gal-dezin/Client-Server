package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;

public class TftpPacketCreator {
    // Method to create a LOGRQ packet with the given username.
    public static byte[] createLogRqPacket(String username) {
        return createPacket((short) 7, username);
    }

    // Method to create a DELRQ packet with the given filename.
    public static byte[] createDelRqPacket(String filename) {
        return createPacket((short) 8, filename);
    }

    // Method to create an RRQ packet with the given filename.
    public static byte[] createRrqPacket(String filename) {
        return createPacket((short) 1, filename);
    }

    // Method to create a WRQ packet with the given filename.
    public static byte[] createWrqPacket(String filename) {
        return createPacket((short) 2, filename);
    }

    // Method to create a DIRQ packet. DIRQ packets do not require additional data.
    public static byte[] createDirqPacket() {
        return shortToBytes((short) 6); // DIRQ has no additional data
    }

    // Method to create a DISC packet. DISC packets do not require additional data.
    public static byte[] createDiscPacket() {
        return shortToBytes((short) 10); // DISC has no additional data
    }

    // Helper method to combine opcode and data into a single packet.
    // Adds a null terminator to the end of the data string for protocol compliance.
    private static byte[] createPacket(short opcode, String data) {
        byte[] opcodeBytes = shortToBytes(opcode);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[opcodeBytes.length + dataBytes.length + 1];
        System.arraycopy(opcodeBytes, 0, packet, 0, opcodeBytes.length);
        System.arraycopy(dataBytes, 0, packet, opcodeBytes.length, dataBytes.length);
        packet[packet.length - 1] = 0; // Null-terminator for the string
        return packet;
    }

    // Converts a short value into a byte array, accounting for endianess.
    private static byte[] shortToBytes(short value) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (value >> 8); // Extracts the high byte
        bytes[1] = (byte) value; // Extracts the low byte
        return bytes;
    }
}
