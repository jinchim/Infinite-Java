package com.jinchim.infinite.protocol;


public abstract class MessageEncoder {

    protected abstract byte[] encode(Message message);

}
