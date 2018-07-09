package org.neo4j.kernel.network;

/**
 * Created by Think on 2018/7/2.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Java序列化工具
 *
 * @author whd
 * @date 2017年11月6日 上午9:24:35
 */
public class SerializeUtil {

    public byte[] encodes(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        byte[] bytes = out.toByteArray();
        return bytes;
    }

    public Object decode(byte[] bytes) throws IOException, ClassNotFoundException {
        // 对象返序列化
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream inn = new ObjectInputStream(in);
        Object obj = inn.readObject();
        return obj;
    }

    /**
     * byte to buf
     *
     * @param bytes
     * @return
     */
    public ByteBuf getBufFromByte(byte[] bytes) {
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        return buf;
    }

    /**
     * buf to byte
     *
     * @param buf
     * @return
     */
    public byte[] getByteFromBuf(ByteBuf buf) {
        int size = buf.readableBytes();
        byte[] bytes = new byte[size];
        buf.readBytes(bytes);
        return bytes;
    }
}

