package bgu.spl.net.api;

import bgu.spl.net.impl.tftp.BlockingConnectionHandler;

import java.util.List;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void connect(int connectionId, BlockingConnectionHandler<T> handler);

    void disconnect(int connectionId);

    boolean existalready(int connectionId);
    public List<Integer> getallconnections();

    void connectname(Integer conncectionid, String name);

    void disconnectname(Integer connectionid);

    public boolean nameexist(Integer connectionid);


}
