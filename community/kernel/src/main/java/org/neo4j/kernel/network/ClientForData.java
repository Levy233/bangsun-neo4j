package org.neo4j.kernel.network;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.queue.BlockingReadHandler;
import org.neo4j.helpers.Exceptions;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.impl.transaction.log.PhysicalTransactionCursor;
import org.neo4j.kernel.impl.transaction.log.ReadableClosablePositionAwareChannel;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryReader;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.kernel.network.exception.ComException;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.kernel.network.message.TransactionStreamResponse;
import org.neo4j.logging.Log;
import org.neo4j.time.Clocks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.neo4j.helpers.NamedThreadFactory.daemon;
import static org.neo4j.kernel.network.Client.INTERNAL_PROTOCOL_VERSION;
import static org.neo4j.kernel.network.Client.applicationProtocolVersion;
import static org.neo4j.kernel.network.Client.getAnother;
import static org.neo4j.kernel.network.ResourcePool.DEFAULT_CHECK_INTERVAL;
import static org.neo4j.kernel.network.ResponseUnpacker.TxHandler.NO_OP_TX_HANDLER;

/**
 * Created by Think on 2018/7/4.
 */
public class ClientForData extends LifecycleAdapter implements ChannelPipelineFactory {

    private ResourcePool<ChannelContext> channelPool;
    //TODO
    private int maxUnusedChannels = 10;
    public static final int MEGA = 1024 * 1024;
    public static final int DEFAULT_FRAME_LENGTH = 16 * MEGA;
    private Log logging;
    private HostnamePort server;
    private ClientBootstrap bootstrap;
    private final int frameLength;
    private final long readTimeout;
    private ResourceReleaser resourcePoolReleaser;
    private final Supplier<LogEntryReader<ReadableClosablePositionAwareChannel>> entryReader;
    private final ResponseUnpacker responseUnpacker;
    private ComExceptionHandler comExceptionHandler;
    private final int chunkSize;

    private static final String BLOCKING_CHANNEL_HANDLER_NAME = "blockingHandler";
    private static final String MONITORING_CHANNEL_HANDLER_NAME = "monitor";

    public ClientForData(LogService logging, List<HostnamePort> members, InstanceId me,int chunkSize, long readTimeOut, ResponseUnpacker unpacker, Supplier<LogEntryReader<ReadableClosablePositionAwareChannel>> entryReader)  {
        this.logging = logging.getInternalLogProvider().getLog(getClass());
        this.server = getAnother(members,me.toIntegerIndex());
        this.chunkSize = chunkSize;
        this.frameLength = DEFAULT_FRAME_LENGTH;
        this.readTimeout = readTimeOut;
        this.entryReader = entryReader;
        this.responseUnpacker = unpacker;
        this.comExceptionHandler = getNoOpComExceptionHandler();
//        ProtocolVersion protocolVersion = getProtocolVersion();
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() throws Throwable {

        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
                newCachedThreadPool(daemon(getClass().getSimpleName() + "-boss@" + server)),
                newCachedThreadPool(daemon(getClass().getSimpleName() + "-worker@" + server))));
        bootstrap.setPipelineFactory(this);
        channelPool = new ResourcePool<ChannelContext>(maxUnusedChannels,
                new ResourcePool.CheckStrategy.TimeoutCheckStrategy(DEFAULT_CHECK_INTERVAL, Clocks.systemClock()),
                new LoggingResourcePoolMonitor(logging)) {
            @Override
            protected ChannelContext create() {
                logging.info(threadInfo() + "Trying to open a new channel to " + server,
                        true);
                // We must specify the origin address in case the server has multiple IPs per interface
                ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(server.getHost(), server.getPort()));
                channelFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
                if (channelFuture.isSuccess()) {
                    logging.info(threadInfo() + "Opened a new channel from " +
                            channelFuture.getChannel().getLocalAddress() + " to " +
                            channelFuture.getChannel().getRemoteAddress());

                    return new ChannelContext(channelFuture.getChannel(), ChannelBuffers.dynamicBuffer(),
                            ByteBuffer.allocate(1024 * 1024));
                }

                Throwable cause = channelFuture.getCause();
                String msg = this.getClass().getSimpleName() + " could not connect to " +
                        server;
                logging.debug(msg, true);
                throw traceComException(new ComException(msg, cause), "Client.start");
            }

            @Override
            protected boolean isAlive(ChannelContext context) {
                return context.channel().isConnected();
            }

            @Override
            protected void dispose(ChannelContext context) {
                org.jboss.netty.channel.Channel channel = context.channel();
                if (channel.isConnected()) {
                    logging.info(threadInfo() + "Closing: " + context + ". " +
                            "Channel pool size is now " + currentSize());
                    channel.close();
                }
            }

            private String threadInfo() {
                return "Thread[" + Thread.currentThread().getId() + ", " + Thread.currentThread().getName() + "] ";
            }
        };
        resourcePoolReleaser = () ->
        {
            if (channelPool != null) {
                channelPool.release();
            }
        };
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
//        pipeline.addLast( MONITORING_CHANNEL_HANDLER_NAME, new MonitorChannelHandler( byteCounterMonitor ) );
        addLengthFieldPipes(pipeline, frameLength);
        BlockingReadHandler<ChannelBuffer> reader =
                new BlockingReadHandler<>(new ArrayBlockingQueue<>(100, false));
        pipeline.addLast(BLOCKING_CHANNEL_HANDLER_NAME, reader);
        return pipeline;
    }

    public static void addLengthFieldPipes(ChannelPipeline pipeline, int frameLength) {
        pipeline.addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(frameLength + 4, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
    }

    @Override
    public void stop() throws Throwable {
        if (channelPool != null) {
            channelPool.close(true);
            bootstrap.releaseExternalResources();
            channelPool = null;
        }

//        comExceptionHandler = getNoOpComExceptionHandler();
        logging.info(toString() + " shutdown", true);
    }

    @Override
    public void shutdown() throws Throwable {

    }

    @SuppressWarnings("unchecked")
    private static BlockingReadHandler<ChannelBuffer> extractBlockingReadHandler(ChannelContext channelContext) {
        ChannelPipeline pipeline = channelContext.channel().getPipeline();
        return (BlockingReadHandler<ChannelBuffer>) pipeline.get(BLOCKING_CHANNEL_HANDLER_NAME);
    }

    private ComExceptionHandler getNoOpComExceptionHandler()
    {
        return exception ->
        {
            if ( ComException.TRACE_HA_CONNECTIVITY )
            {
                String noOpComExceptionHandler = "NoOpComExceptionHandler";
                //noinspection ThrowableResultOfMethodCallIgnored
                traceComException( exception, noOpComExceptionHandler );
            }
        };
    }

    private ComException traceComException(ComException exception, String tracePoint) {
        return exception.traceComException(logging, tracePoint);
    }

    public <R> Response<R> sendRequest(RequestContext context,
                                          Serializer serializer, Deserializer<R> deserializer) {
        return sendRequest(context, serializer, deserializer, null, NO_OP_TX_HANDLER);
    }

    protected <R> Response<R> sendRequest(
//            RequestType<T> type,
            RequestContext context,
            Serializer serializer, Deserializer<R> deserializer,
            StoreId specificStoreId, ResponseUnpacker.TxHandler txHandler) {
        ChannelContext channelContext = acquireChannelContext();

        Throwable failure = null;
        try {
//            requestMonitor.beginRequest( channelContext.channel().getRemoteAddress(), type, context );

            // Request
            serializeRequest(channelContext.channel(), channelContext.output(), context, serializer);

            // Response
            Response<R> response = deserializeResponse(extractBlockingReadHandler(channelContext),
                    channelContext.input(), readTimeout, deserializer, resourcePoolReleaser,
                    entryReader.get());
            responseUnpacker.unpackResponse(response, txHandler);
            return response;
        } catch (ComException e) {
            failure = e;
            comExceptionHandler.handle(e);
            throw traceComException(e, "Client.sendRequest");
        } catch (Throwable e) {
            failure = e;
            throw Exceptions.launderedException(ComException.class, e);
        } finally {
            /*
             * Otherwise the user must call response.close() to prevent resource leaks.
             */
            if (failure != null) {
                dispose(channelContext);
            }
//            requestMonitor.endRequest( failure );
        }
    }

    private void dispose(ChannelContext channelContext) {
        channelContext.channel().close().awaitUninterruptibly();
        if (channelPool != null) {
            channelPool.release();
        }
    }

    private ChannelContext acquireChannelContext(
//            RequestType<T> type
    ) {
        try {
            if (channelPool == null) {
                throw new ComException(String.format("Client for %s is stopped", server.toString()));
            }

            // Calling acquire is dangerous since it may be a blocking call... and if this
            // thread holds a lock which others may want to be able to communicate with
            // the server things go stiff.
            ChannelContext result = channelPool.acquire();
            if (result == null) {
//                logging.error( "Unable to acquire new channel for " + type );
                throw traceComException(
                        new ComException("Unable to acquire new channel"),
                        "Client.acquireChannelContext");
            }
            return result;
        } catch (Throwable e) {
            throw Exceptions.launderedException(ComException.class, e);
        }
    }

    public void serializeRequest(Channel channel, ChannelBuffer buffer, RequestContext ctx,
                                 Serializer payload) throws IOException {
        buffer.clear();
        ChunkingChannelBuffer chunkingBuffer = new ChunkingChannelBuffer(buffer,
                channel, chunkSize, INTERNAL_PROTOCOL_VERSION, applicationProtocolVersion);
        chunkingBuffer.writeByte(5);
        writeContext(ctx, chunkingBuffer);
        payload.write(chunkingBuffer);
        chunkingBuffer.done();
    }

    private void writeContext(RequestContext context, ChannelBuffer targetBuffer) {
        targetBuffer.writeLong(context.getEpoch());
        targetBuffer.writeInt(context.machineId());
        targetBuffer.writeInt(context.getEventIdentifier());
        long tx = context.lastAppliedTransaction();
        targetBuffer.writeLong(tx);
        targetBuffer.writeLong(context.getChecksum());
    }

    protected StoreId readStoreId(ChannelBuffer source, ByteBuffer byteBuffer) {
        byteBuffer.clear();
        byteBuffer.limit(8 + 8 + 8 + 8 + 8); // creation time, random id, store version, upgrade time, upgrade id
        source.readBytes(byteBuffer);
        byteBuffer.flip();
        // read order matters - see Server.writeStoreId() for version 2.1.4
        long creationTime = byteBuffer.getLong();
        long randomId = byteBuffer.getLong();
        long storeVersion = byteBuffer.getLong();
        long upgradeTime = byteBuffer.getLong();
        long upgradeId = byteBuffer.getLong();
        return new StoreId(creationTime, randomId, storeVersion, upgradeTime, upgradeId);
    }

    public <PAYLOAD> Response<PAYLOAD> deserializeResponse(BlockingReadHandler<ChannelBuffer> reader,
                                                           ByteBuffer input, long timeout,
                                                           Deserializer<PAYLOAD> payloadDeserializer,
                                                           ResourceReleaser channelReleaser,
                                                           final LogEntryReader<ReadableClosablePositionAwareChannel> entryReader) throws IOException {
        final DechunkingChannelBuffer dechunkingBuffer = new DechunkingChannelBuffer(reader, timeout,
                INTERNAL_PROTOCOL_VERSION, applicationProtocolVersion);

        PAYLOAD response = payloadDeserializer.read(dechunkingBuffer, input);
        StoreId storeId = readStoreId(dechunkingBuffer, input);

        // Response type is what previously was a byte saying how many data sources there were in the
        // coming transaction stream response. For backwards compatibility we keep it as a byte and we introduce
        // the transaction obligation response type as -1
        byte responseType = dechunkingBuffer.readByte();
//        if ( responseType == TransactionObligationResponse.RESPONSE_TYPE )
//        {
//            // It is a transaction obligation response
//            long obligationTxId = dechunkingBuffer.readLong();
//            return new TransactionObligationResponse<>( response, storeId, obligationTxId, channelReleaser );
//        }

        // It's a transaction stream in this response
        TransactionStream transactions = visitor ->
        {
            NetworkReadableClosableChannel channel = new NetworkReadableClosableChannel(dechunkingBuffer);

            try (PhysicalTransactionCursor<ReadableClosablePositionAwareChannel> cursor =
                         new PhysicalTransactionCursor<>(channel, entryReader)) {
                while (cursor.next() && !visitor.visit(cursor.get())) {
                }
            }
        };
        return new TransactionStreamResponse<>(response, storeId, transactions, channelReleaser);
    }

}
