package bgu.spl.net.api;

import bgu.spl.net.impl.tftp.BlockingConnectionHandler;

public interface BidiMessagingProtocol<T>  {
	/**
	 * Used to initiate the current client protocol with it's personal connection ID and the connections implementation
	**/
    void start(int connectionId, Connections<T> connections);
    
    void process(T message, int opcode, BlockingConnectionHandler<T> handler);
	
	/**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}
