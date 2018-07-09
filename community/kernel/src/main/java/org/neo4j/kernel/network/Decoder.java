package org.neo4j.kernel.network;

/**
 * Created by Think on 2018/7/2.
 */
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * 解码
 *
 * @author whd
 * @date 2017年11月6日 下午1:56:45
 */
public class Decoder extends ByteToMessageDecoder {
    private static final int LENGTH = 4;

    /**
     * 解码时传进来的参数是ByteBuf也就是Netty的基本单位，而传出的是解码后的Object
     */
    @Override
    protected void decode(ChannelHandlerContext arg0, ByteBuf in, List<Object> out) throws Exception {
        SerializeUtil util = new SerializeUtil();
        int size = in.readableBytes();
        // 这里判断大小是为了tcp的粘包问题，这个之后再单独学习
        if (size > Decoder.LENGTH) {
            byte[] bytes = util.getByteFromBuf(in);
            Object info = util.decode(bytes);
            out.add(info);
        }

    }
}

