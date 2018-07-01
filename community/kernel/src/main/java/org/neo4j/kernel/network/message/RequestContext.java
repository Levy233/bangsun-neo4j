package org.neo4j.kernel.network.message;

/**
 * Created by Think on 2018/6/26.
 */
public class RequestContext implements Message {

    private long lastAppliedTransaction;

    @Override
    public void handle(Handler handler) {

    }
    public long lastAppliedTransaction()
    {
        return lastAppliedTransaction;
    }
}
