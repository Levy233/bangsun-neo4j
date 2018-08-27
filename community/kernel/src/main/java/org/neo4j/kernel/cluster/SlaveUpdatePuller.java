package org.neo4j.kernel.cluster;

import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.ClientForData;
import org.neo4j.kernel.network.Deserializer;
import org.neo4j.kernel.network.RequestContextFactory;
import org.neo4j.kernel.network.Serializer;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.kernel.network.state.StateMachine;
import org.neo4j.kernel.network.state.StoreState;
import org.neo4j.logging.Log;
import org.neo4j.scheduler.JobScheduler;

/**
 * Created by Think on 2018/6/15.
 */
public class SlaveUpdatePuller implements Runnable, Lifecycle, JobScheduler.CancelListener {

    private boolean needToPull = false;

    public StateMachine stateMachine;
    private JobScheduler jobScheduler;
    private ClientForData client;
    private RequestContextFactory requestContextFactory;
    private volatile boolean halted = false;
    private LogService logging;
    private Log log;
    public static final Serializer EMPTY_SERIALIZER = buffer -> {
    };
    public static final Deserializer<Void> VOID_DESERIALIZER = (buffer, temporaryBuffer) -> null;

    public SlaveUpdatePuller(StateMachine stateMachine, ClientForData client, RequestContextFactory requestContextFactory, JobScheduler jobScheduler, LogService logging) {
        this.stateMachine = stateMachine;
        this.client = client;
        this.requestContextFactory = requestContextFactory;
        this.jobScheduler = jobScheduler;
        this.logging = logging;
        this.log = logging.getInternalLog(getClass());
    }

    @Override
    public void run() {

        while (!halted) {
            try {
                periodicallyPullUpdates();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void periodicallyPullUpdates() throws InterruptedException {
        checkIfNeedToPull();
        if (needToPull) {
            log.info("need to pull updates");
            tryToPullUpdate();
        }
    }

    private void checkIfNeedToPull() throws InterruptedException {
        switch (stateMachine.getStoreState()) {
            case newer:
                needToPull = true;
                break;
            default:
                needToPull = false;
                Thread.sleep(100);
        }
    }

    private void tryToPullUpdate() throws InterruptedException {
        try{
            stateMachine.setStoreState(StoreState.pulling);
            RequestContext context = requestContextFactory.newRequestContext();
            client.sendRequest(context, EMPTY_SERIALIZER, VOID_DESERIALIZER);
        }catch (Throwable e){
            stateMachine.setStoreState(StoreState.start);
        }
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() throws Throwable {
        JobScheduler.JobHandle handle = jobScheduler.schedule(JobScheduler.Groups.pullUpdates, this);
        handle.registerCancelListener(this);
    }

    @Override
    public void stop() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {

    }

    @Override
    public void cancelled(boolean mayInterruptIfRunning) {
        halted = true;
    }
}
