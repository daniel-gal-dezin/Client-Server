package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;

public class CommandState {
    private String filename;
    private boolean dirqActive = false; // Track if a DIRC command has been sent
    private boolean disconnecting = false; // Track if a DISC command has been sent
    private boolean isLogin = false; // Track if a LOGRQ command has been sent
    private boolean transferFile = false; // Track if a WRQ command has been sent



    private StringBuilder dirList = new StringBuilder();

    public synchronized void setFilename(String filename) {
        this.filename = filename;
    }

    public synchronized String getFilename() {
        return filename;
    }

    public synchronized boolean isDirqActive() {
        return dirqActive;
    }

    public synchronized void setDirqActive(boolean dirqActive) {
        this.dirqActive = dirqActive;
    }

    public synchronized void appendToDirList(byte[] data) {
        String files = new String(data, StandardCharsets.UTF_8).trim();
        if (!files.isEmpty()) {
            // Assuming file names are separated by spaces and ending with a space.
            String[] filenames = files.split("\0");
            for (String filename : filenames) {
                // Append each filename followed by a newline character
                dirList.append(filename).append("\n");
            }
        }
    }
    public synchronized String getDirList() {
        return dirList.toString();
    }

    public synchronized void resetDirList() {
        dirList.setLength(0);
    }

    public synchronized boolean isDisconnecting() {
        return disconnecting;
    }

    public synchronized void setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
    }

    public synchronized boolean isLogin() {
        return isLogin;
    }

    public synchronized void setLogin(boolean isLogin) {
        this.isLogin = isLogin;
    }

    public synchronized boolean isTransferFile() {
        return transferFile;
    }

    public synchronized void setTransferFile(boolean transferFile) {
        this.transferFile = transferFile;
    }
}
