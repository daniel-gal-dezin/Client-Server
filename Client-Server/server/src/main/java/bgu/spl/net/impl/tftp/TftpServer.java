package bgu.spl.net.impl.tftp;

import bgu.spl.net.impl.tftp.*;



public class TftpServer {

    public static void main(String[] args) {
        TftpBaseServer<byte[]> a = new TftpBaseServer<>(7777, TftpProtocol::new, TftpEncoderDecoder::new);
        a.serve();

    }

}