package com.alishangtian.mtimer.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;

import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;


/**
 * @Desc NettyDecoder
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    private final ClassResolver classResolver;

    /**
     * Creates a new decoder whose maximum object size is {@code 1048576}
     * bytes.  If the size of the received object is greater than
     * {@code 1048576} bytes, a {@link StreamCorruptedException} will be
     * raised.
     *
     * @param classResolver the {@link ClassResolver} to use for this decoder
     */
    public NettyDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    /**
     * Creates a new decoder with the specified maximum object size.
     *
     * @param maxObjectSize the maximum byte length of the serialized object.
     *                      if the length of the received object is greater
     *                      than this value, {@link StreamCorruptedException}
     *                      will be raised.
     * @param classResolver the {@link ClassResolver} which will load the class
     *                      of the serialized object
     */
    public NettyDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        ObjectInputStream ois = new CompactObjectInputStream(new ByteBufInputStream(frame, true), classResolver);
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }
}

