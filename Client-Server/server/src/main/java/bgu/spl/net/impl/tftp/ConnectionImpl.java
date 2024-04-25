package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.Connections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, BlockingConnectionHandler<T>> connects = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> names = new ConcurrentHashMap<>();




    @Override
    public boolean send(int connectionId, T msg) {
        BlockingConnectionHandler<T> handler = connects.get(connectionId);
//        if(handler = null){ what if get null
////
////        }
        handler.send(msg);
        return true;
    }

    @Override
    public void connect(int connectionId, BlockingConnectionHandler<T> handler) {
        connects.put(connectionId,handler);


    }

    @Override
    public void disconnect(int connectionId) {
        connects.remove(connectionId);

    }
    public boolean existalready(int connectionId) {
        BlockingConnectionHandler<T> exist = connects.get(connectionId);
        if (exist == null){
            return false;
        }
        else
            return true;
    }
    public List<Integer> getallconnections() {

      List<Integer> total = new ArrayList<>(connects.keySet());
      return total;


    }

    @Override
    public void connectname(Integer conncectionid, String name) {
        names.put(conncectionid,name);


    }

    @Override
    public void disconnectname(Integer connectionid) {
        names.remove(connectionid);
    }


    public boolean nameexist(Integer connectionid) {
        String exist = names.get(connectionid);
        if (exist == null){
            return false;
        }
        else
            return true;


    }






}
