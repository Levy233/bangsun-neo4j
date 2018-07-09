package org.neo4j.kernel.network;

/**
 * Created by Think on 2018/7/2.
 */
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * netty中的编码
 *
 * @author whd
 * @date 2017年12月3日 下午7:43:23
 */
public class Encoder extends MessageToByteEncoder<Object> {
    /**
     * 我们知道在计算机中消息的交互都是byte 但是在netty中进行了封装所以在netty中基本的传递类型是ByteBuf
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        SerializeUtil util = new SerializeUtil();
        byte[] bytes = util.encodes(in);
        out.writeBytes(bytes);
    }

}
