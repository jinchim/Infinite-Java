package com.jinchim.infinite.protocol;


public abstract class MessageDecoder {

    protected abstract Message decode(byte[] bytes);

}
