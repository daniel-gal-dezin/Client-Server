package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;


public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 10]; // 1KB buffer
    private int len = 0;
    private boolean readingOpcode = true;
    private short opcode = -1;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (readingOpcode) {
            pushByte(nextByte);
            if (len == 2) {
                opcode = (short) (((short) bytes[0] & 0xFF) << 8 | (short) (bytes[1] & 0XFF));
                len = 0;
                readingOpcode = false;
            }
        } else if(opcode == 7 ||opcode == 1 || opcode== 2 ||   opcode == 8 ){
            pushByte(nextByte);
            if (nextByte == 0) {
                byte[] message = popByte();
                readingOpcode = true;
                opcode = -1;
                len = 0;
                return message;
            }
        }
        else if (opcode==5||opcode==9)
        {
            pushByte(nextByte);
            if (nextByte == 0 && len>1) {
                byte[] message = popByte();
                readingOpcode = true;
                opcode = -1;
                len = 0;
                return message;
            }
        }
        else if (opcode == 3) {
            pushByte(nextByte);

            if(len<2) {
            }
            else {
                int size = (short) (((short) bytes[0] & 0xFF) << 8 | (short) (bytes[1] & 0XFF));
                if (len == size + 4) {
                    byte[] message = popByte();
                    readingOpcode = true;
                    opcode = -1;
                    len = 0;
                    return message;
                }
            }

        } else if (opcode == 4) {
            pushByte(nextByte);
            if (len == 2){
                byte[] message = popByte();
                readingOpcode = true;
                opcode = -1;
                len = 0;
                return message;
            }


        }


        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    @Override
    public int getopcode() {
        return opcode;
    }

    @Override
    public boolean havinopcode() {
        return readingOpcode;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            byte[] newBytes = new byte[len * 2];
            System.arraycopy(bytes, 0, newBytes, 0, len);
            bytes = newBytes;
        }
        bytes[len++] = nextByte;
    }

    private byte[] popByte() {
        byte[] result = new byte[len];
        System.arraycopy(bytes, 0, result, 0, len);
        len = 0;
        return result;
    }


    public void setOpcode(short opcode) {
        this.opcode = opcode;
    }

    @Override
    public void setreadingopcode(boolean b) {
        this.readingOpcode = b;
    }


}