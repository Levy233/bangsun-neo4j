package org.neo4j.kernel.network;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.neo4j.helpers.Exceptions;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.exception.IllegalProtocolVersionException;
import org.neo4j.kernel.network.handle.ServerHandler;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import sun.util.logging.resources.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.*;
import static org.neo4j.helpers.NamedThreadFactory.daemon;
import static org.neo4j.helpers.NamedThreadFactory.named;
import static org.neo4j.kernel.network.Client.INTERNAL_PROTOCOL_VERSION;
import static org.neo4j.kernel.network.Client.applicationProtocolVersion;
import static org.neo4j.kernel.network.ClientForData.DEFAULT_FRAME_LENGTH;
import static org.neo4j.kernel.network.DechunkingChannelBuffer.assertSameProtocolVersion;

/**
 * Created by Think on 2018/7/6.
 */
public class ServerForData extends SimpleChannelHandler implements Lifecycle,ChannelPipelineFactory,ChannelCloser {

    @Override
    public void tryToCloseChannel(Channel channel) {

    }

    public interface Configuration {
        HostnamePort me();

        long getOldChannelThreshold();

        int getMaxConcurrentTransactions();

        int getChunkSize();
    }
    private Log logging;
    private Configuration config;
    private ServerBootstrap serverBootstrap;
    private final int frameLength = DEFAULT_FRAME_LENGTH;
    private ChannelGroup channelGroup;
    private int chunkSize;
    private ResponsePacker responsePacker;
    private ExecutorService targetCallExecutor;
    private ExecutorService unfinishedTransactionExecutor;
    private ScheduledExecutorService silentChannelExecutor;
    private final IdleChannelReaper connectedSlaveChannels;
    private final Map<Channel, PartialRequest> partialRequests = new ConcurrentHashMap<>();
    public static final ObjectSerializer<Void> VOID_SERIALIZER = (responseObject, result) -> {
    };

    public ServerForData(Configuration config,LogProvider logProvider, Clock clock,ResponsePacker responsePacker){
        this.logging = logProvider.getLog(getClass());
        this.config = config;
        this.responsePacker = responsePacker;
        this.connectedSlaveChannels = new IdleChannelReaper(this, logProvider, clock, config.getOldChannelThreshold());
    }
    @Override
    public void init() throws Throwable {
        chunkSize = config.getChunkSize();
        assertChunkSizeIsWithinFrameSize( chunkSize, frameLength );
        String className = getClass().getSimpleName();
        targetCallExecutor = newCachedThreadPool( named( className + ":" + config.me().getPort() ) );
        unfinishedTransactionExecutor = newScheduledThreadPool( 2, named( "Unfinished transactions" ) );
        silentChannelExecutor = newSingleThreadScheduledExecutor( named( "Silent channel reaper" ) );
        silentChannelExecutor.scheduleWithFixedDelay( connectedSlaveChannels, 5, 5, TimeUnit.SECONDS );

    }

    @Override
    public void start() throws Throwable {
        String className = getClass().getSimpleName();
        ExecutorService boss = Executors.newCachedThreadPool(daemon( "Boss-" + className ));
        ExecutorService worker = Executors.newCachedThreadPool(daemon( "Worker-" + className ));
        serverBootstrap = new ServerBootstrap( new NioServerSocketChannelFactory(
                boss, worker, config.getMaxConcurrentTransactions() ) );
        serverBootstrap.setPipelineFactory( this );
//        ServerHandler handler = new ServerHandler(aliveSlaves, config, logging,channelGroup, Clocks.systemClock(),stateMachine,responsePacker);
        try
        {
            Channel channel = serverBootstrap.bind(new InetSocketAddress(config.me().getPort()));
            channelGroup = new DefaultChannelGroup();
            channelGroup.add( channel );
//            msgLog.info( className + " communication server started and bound to " + socketAddress );
        }
        catch ( Exception ex )
        {
//            msgLog.error( "Failed to bind server to " + socketAddress, ex );
            serverBootstrap.releaseExternalResources();
            targetCallExecutor.shutdownNow();
            unfinishedTransactionExecutor.shutdownNow();
            silentChannelExecutor.shutdownNow();
            throw new IOException( ex );
        }
    }

    @Override
    public void channelOpen( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception
    {
        logging.info("Server: channelOpened: Local address: "+ ctx.getChannel().getLocalAddress()+" Remote address: "+ ctx.getChannel().getRemoteAddress());
        channelGroup.add( e.getChannel() );
    }

    @Override
    public void messageReceived( ChannelHandlerContext ctx, MessageEvent event )
            throws Exception
    {
        try
        {
            ChannelBuffer message = (ChannelBuffer) event.getMessage();
            handleRequest( message, event.getChannel() );
        }
        catch ( Throwable e )
        {
            logging.error( "Error handling request", e );

            // Attempt to reply to the client
            ChunkingChannelBuffer buffer = newChunkingBuffer( event.getChannel() );
            buffer.clear( /* failure = */true );
            writeFailureResponse( e, buffer );

            ctx.getChannel().close();
            tryToCloseChannel( ctx.getChannel() );
            throw Exceptions.launderedException( e );
        }
    }

    private void writeFailureResponse(Throwable exception, ChunkingChannelBuffer buffer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(exception);
            out.close();
            buffer.writeBytes(bytes.toByteArray());
            buffer.done();
            logging.info("Sended cause of error to client");
        } catch (IOException e) {
            logging.warn("Couldn't send cause of error to client", exception);
        }
    }

    private void handleRequest(ChannelBuffer buffer, final Channel channel) {
        Byte continuation = readContinuationHeader(buffer, channel);
        if (continuation == null) {
            return;
        }
        if (continuation == ChunkingChannelBuffer.CONTINUATION_MORE) {
            PartialRequest partialRequest = partialRequests.get(channel);
            if (partialRequest == null) {
                // This is the first chunk in a multi-chunk request
//                RequestType<T> type = getRequestContext( buffer.readByte() );
                buffer.readByte();
                RequestContext context = readContext(buffer);
                ChannelBuffer targetBuffer = mapSlave(channel, context);
                partialRequest = new PartialRequest(context, targetBuffer);
                partialRequests.put(channel, partialRequest);
            }
            partialRequest.add(buffer);
        } else {
            PartialRequest partialRequest = partialRequests.remove(channel);
//            RequestType<T> type;
            RequestContext context;
            ChannelBuffer targetBuffer;
            ChannelBuffer bufferToReadFrom;
            ChannelBuffer bufferToWriteTo;
            if (partialRequest == null) {
                // This is the one and single chunk in the request
//                type = getRequestContext( buffer.readByte() );
                buffer.readByte();
                context = readContext(buffer);
                targetBuffer = mapSlave(channel, context);
                bufferToReadFrom = buffer;
                bufferToWriteTo = targetBuffer;
            } else {
                // This is the last chunk in a multi-chunk request
//                type = partialRequest.type;
                context = partialRequest.context;
                targetBuffer = partialRequest.buffer;
                partialRequest.add(buffer);
                bufferToReadFrom = targetBuffer;
                bufferToWriteTo = ChannelBuffers.dynamicBuffer();
            }

            bufferToWriteTo.clear();
            ChunkingChannelBuffer chunkingBuffer = newChunkingBuffer(bufferToWriteTo, channel, chunkSize,
                    INTERNAL_PROTOCOL_VERSION, applicationProtocolVersion);
            submitSilent(targetCallExecutor, new TargetCaller(channel, context, chunkingBuffer,
                    bufferToReadFrom));
        }
    }

    private ChannelBuffer mapSlave(Channel channel, RequestContext slave) {
        // Checking for machineId -1 excludes the "empty" slave contexts
        // which some communication points pass in as context.
        if (slave != null && slave.machineId() != RequestContext.EMPTY.machineId()) {
            connectedSlaveChannels.add(channel, slave);
        }
        return ChannelBuffers.dynamicBuffer();
    }

    private IdleChannelReaper.Request unmapSlave(Channel channel) {
        return connectedSlaveChannels.remove(channel);
    }


    private RequestContext readContext(ChannelBuffer buffer) {
        long sessionId = buffer.readLong();
        int machineId = buffer.readInt();
        int eventIdentifier = buffer.readInt();
        long neoTx = buffer.readLong();
        long checksum = buffer.readLong();

        RequestContext readRequestContext =
                new RequestContext(sessionId, machineId, eventIdentifier, neoTx, checksum);

        // verify checksum only if there are transactions committed in the store
        if (neoTx > TransactionIdStore.BASE_TX_ID) {
//            txVerifier.assertMatch( neoTx, checksum );
        }
        return readRequestContext;
    }

    private Byte readContinuationHeader(ChannelBuffer buffer, final Channel channel) {
        byte[] header = new byte[2];
        buffer.readBytes(header);
        try {   // Read request header and assert correct internal/application protocol version
            assertSameProtocolVersion(header, INTERNAL_PROTOCOL_VERSION, applicationProtocolVersion);
        } catch (final IllegalProtocolVersionException e) {   // Version mismatch, fail with a good exception back to the client
            submitSilent(targetCallExecutor, () -> writeFailureResponse(e, newChunkingBuffer(channel)));
            return null;
        }
        return (byte) (header[0] & 0x1);
    }

    private void submitSilent(ExecutorService service, Runnable job) {
        try {
            service.submit(job);
        } catch (RejectedExecutionException e) {   // Don't scream and shout if we're shutting down, because a rejected execution
            // is expected at that time.
//            if ( !shuttingDown )
//            {
            throw e;
//            }
        }
    }

    private ChunkingChannelBuffer newChunkingBuffer(Channel channel) {
        return newChunkingBuffer(ChannelBuffers.dynamicBuffer(), channel, chunkSize, INTERNAL_PROTOCOL_VERSION,
                applicationProtocolVersion);
    }

    private ChunkingChannelBuffer newChunkingBuffer(ChannelBuffer bufferToWriteTo, Channel channel, int capacity,
                                                    byte internalProtocolVersion, byte applicationProtocolVersion) {
        return new ChunkingChannelBuffer(bufferToWriteTo, channel, capacity, internalProtocolVersion,
                applicationProtocolVersion);
    }

    @Override
    public void stop() throws Throwable {
        channelGroup.close().awaitUninterruptibly();
        serverBootstrap.releaseExternalResources();
    }

    @Override
    public void shutdown() throws Throwable {
        channelGroup.close().awaitUninterruptibly();
        serverBootstrap.releaseExternalResources();
        targetCallExecutor.shutdown();
        targetCallExecutor.awaitTermination( 10, TimeUnit.SECONDS );
        unfinishedTransactionExecutor.shutdown();
        unfinishedTransactionExecutor.awaitTermination( 10, TimeUnit.SECONDS );
        silentChannelExecutor.shutdown();
        silentChannelExecutor.awaitTermination( 10, TimeUnit.SECONDS );
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
//        pipeline.addLast( "monitor", new MonitorChannelHandler( byteCounterMonitor ) );
        addLengthFieldPipes( pipeline, frameLength );
        pipeline.addLast( "serverHandler", this );
        return pipeline;
    }

    private static void addLengthFieldPipes(ChannelPipeline pipeline, int frameLength)
    {
        pipeline.addLast( "frameDecoder",
                new LengthFieldBasedFrameDecoder( frameLength + 4, 0, 4, 0, 4 ) );
        pipeline.addLast( "frameEncoder", new LengthFieldPrepender( 4 ) );
    }

    private static void assertChunkSizeIsWithinFrameSize(int chunkSize, int frameLength)
    {
        if ( chunkSize > frameLength )
        {
            throw new IllegalArgumentException( "Chunk size " + chunkSize +
                    " needs to be equal or less than frame length " + frameLength );
        }
    }

    private static void writeStoreId(StoreId storeId, ChannelBuffer targetBuffer) {
        targetBuffer.writeLong(storeId.getCreationTime());
        targetBuffer.writeLong(storeId.getRandomId());
        targetBuffer.writeLong(storeId.getStoreVersion());
        targetBuffer.writeLong(storeId.getUpgradeTime());
        targetBuffer.writeLong(storeId.getUpgradeId());
    }

    private void tryToFinishOffChannel(Channel channel, RequestContext slave) {
        try {
//            stopConversation(slave);
            unmapSlave(channel);
            logging.info("");
        } catch (Throwable failure) // Unknown error trying to finish off the tx
        {
            submitSilent(unfinishedTransactionExecutor, newTransactionFinisher(slave));
            logging.warn("Could not finish off dead channel", failure);
        }
    }

    private Runnable newTransactionFinisher(final RequestContext slave) {
        return new Runnable() {
            @Override
            public void run() {
                try {
//                    stopConversation(slave);
                } catch (Throwable e) {
                    // Introduce some delay here. it becomes like a busy wait if it never succeeds
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e1) {
                        Thread.interrupted();
                    }
                    unfinishedTransactionExecutor.submit(this);
                }
            }
        };
    }

    private class PartialRequest {
        final RequestContext context;
        final ChannelBuffer buffer;
//        final RequestType<T> type;

        PartialRequest(RequestContext context, ChannelBuffer buffer) {
//            this.type = type;
            this.context = context;
            this.buffer = buffer;
        }

        public void add(ChannelBuffer buffer) {
            this.buffer.writeBytes(buffer);
        }
    }

    private class TargetCaller implements Response.Handler, Runnable {
        //        private final RequestType<T> type;
        private final Channel channel;
        private final RequestContext context;
        private final ChunkingChannelBuffer targetBuffer;
        private final ChannelBuffer bufferToReadFrom;

        TargetCaller(Channel channel, RequestContext context,
                     ChunkingChannelBuffer targetBuffer, ChannelBuffer bufferToReadFrom) {
            this.channel = channel;
            this.context = context;
            this.targetBuffer = targetBuffer;
            this.bufferToReadFrom = bufferToReadFrom;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            Response<Void> response = null;
            try {
                unmapSlave(channel);
                response = packTransactionStreamResponse(context, null);
                VOID_SERIALIZER.write(response.response(), targetBuffer);
                writeStoreId(response.getStoreId(), targetBuffer);
                response.accept(this);
                targetBuffer.done();
            } catch (Throwable e) {
                targetBuffer.clear(true);
                writeFailureResponse(e, targetBuffer);
                tryToFinishOffChannel(channel, context);
                throw Exceptions.launderedException(e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        @Override
        public void obligation(long txId) throws IOException {
            targetBuffer.writeByte(-1);
            targetBuffer.writeLong(txId);
        }

        @Override
        public Visitor<CommittedTransactionRepresentation, Exception> transactions() {
            targetBuffer.writeByte(1);
            return new CommittedTransactionSerializer(new NetworkFlushableChannel(targetBuffer));
        }
    }
    public <T> Response<T> packTransactionStreamResponse(RequestContext context, T response) {
        return responsePacker.packTransactionStreamResponse(context, response);
    }
}
