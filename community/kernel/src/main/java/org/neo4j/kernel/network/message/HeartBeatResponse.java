package org.neo4j.kernel.network.message;

import java.io.Serializable;

/**
 * Created by Think on 2018/7/3.
 */
public class HeartBeatResponse implements Message,Serializable{
    private static final long nSerialVerUID = 89098401L;
    private String response;

    public HeartBeatResponse(String response){
        this.response = response;
    }

    @Override
    public void handle(Handler handler) throws Exception {

    }

    @Override
    public String toString(){
        return response;
    }
}
