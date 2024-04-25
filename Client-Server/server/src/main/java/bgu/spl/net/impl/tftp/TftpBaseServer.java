package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.function.Supplier;

public class TftpBaseServer<T> extends BaseServer<T> {
    public TftpBaseServer( int port,
                           Supplier<BidiMessagingProtocol<T>> protocolFactory,
                           Supplier<MessageEncoderDecoder<T>> encdecFactory) {
        super(port, protocolFactory, encdecFactory);
    }

    @Override
    protected void execute(BlockingConnectionHandler<T> handler) {
        new Thread(handler).start();
    }
}
