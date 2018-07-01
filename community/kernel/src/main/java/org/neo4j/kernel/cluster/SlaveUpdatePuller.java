package org.neo4j.kernel.cluster;

import io.netty.channel.Channel;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.message.Message;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.kernel.network.state.StateMachine;
import org.neo4j.scheduler.JobScheduler;

/**
 * Created by Think on 2018/6/15.
 */
public class SlaveUpdatePuller implements Runnable,Lifecycle,JobScheduler.CancelListener{

    private boolean needToPull = false;

    public StateMachine stateMachine;
    private JobScheduler jobScheduler;
    private Channel channel;

    public SlaveUpdatePuller(StateMachine stateMachine ,Channel channel,JobScheduler jobScheduler){
        this.stateMachine = stateMachine;
        this.channel = channel;
        this.jobScheduler = jobScheduler;
    }
    @Override
    public void run() {
        periodicallyPullUpdates();
    }

    private void periodicallyPullUpdates(){
        checkIfNeedToPull();
        if(needToPull){
            tryToPullUpdate();
        }
    }

    private void checkIfNeedToPull(){
        switch (stateMachine.getStoreState()){
            case newer:
                needToPull=true;
                break;
            default:
                needToPull=false;
        }
    }

    private void tryToPullUpdate(){
        RequestContext context = new RequestContext();
        stateMachine.getTransactionIdStore().getLastCommittedTransactionId();
        sendRequest(context);
    }

    private void sendRequest(Message msg){
        channel.writeAndFlush(msg);
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() throws Throwable {
        JobScheduler.JobHandle handle = jobScheduler.schedule( JobScheduler.Groups.pullUpdates, this );
        handle.registerCancelListener( this );
    }

    @Override
    public void stop() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {

    }

    @Override
    public void cancelled(boolean mayInterruptIfRunning) {

    }
}
