package bgu.spl.net.impl.tftp;



import java.io.File;
import java.util.concurrent.ConcurrentHashMap;






public class FileTransferState {

    private int currentBlockNumber;

    private boolean currentupload;
    private boolean currentdownload;

    private byte[] rrqpacket;
    private byte[] wrqpacket;
    private byte[] dirqpacket;




    public FileTransferState(){

        this.currentBlockNumber= 0;
        this.currentupload = false;
        this.currentdownload =false;
        this.rrqpacket = null;
        this.wrqpacket = null;


    }


    public int getCurrentBlockNumber() {
        return currentBlockNumber;
    }

    public void setCurrentBlockNumber(int currentBlockNumber) {
        this.currentBlockNumber = currentBlockNumber;
    }




    public boolean isCurrentupload() {
        return currentupload;
    }

    public void setCurrentupload(boolean currentupload) {
        this.currentupload = currentupload;
    }

    public boolean isCurrentdownload() {
        return currentdownload;
    }

    public void setCurrentdownload(boolean currentdownload) {
        this.currentdownload = currentdownload;
    }

    public byte[] getRrqpacket() {
        return rrqpacket;
    }

//    public void setRrqpacket(byte[] rrqpacket) {
//        this.rrqpacket = rrqpacket;
//    }

    public byte[] getWrqpacket() {
        return wrqpacket;
    }

    public void setWrqpacket(byte[] wrqpacket) {
        this.wrqpacket = wrqpacket;
    }

    public byte[] getDirqpacket() {
        return dirqpacket;
    }

    public void setDirqpacket(byte[] dirqpacket) {
        this.dirqpacket = dirqpacket;
    }
}
