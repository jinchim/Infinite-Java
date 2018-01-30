package com.jinchim.infinite.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class ProtocolEncoder extends MessageToMessageEncoder<Protocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Protocol in, List<Object> out) throws Exception {
        byte[] bytes = in.toByteArray();
        ByteBuf buf = Unpooled.buffer();
        out.add(buf.writeBytes(bytes));
    }

}
