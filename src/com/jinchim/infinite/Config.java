package com.jinchim.infinite;


import com.jinchim.infinite.protocol.MessageDecoder;
import com.jinchim.infinite.protocol.MessageEncoder;
import com.jinchim.infinite.protocol.message.string.StringDecoder;
import com.jinchim.infinite.protocol.message.string.StringEncoder;

public abstract class Config {

    // 解码器，默认为 StringDecoder
    private static MessageDecoder decoder = new StringDecoder();
    // 编码器，默认为 StringEncoder
    private static MessageEncoder encoder = new StringEncoder();

    public static MessageDecoder getDecoder() {
        return decoder;
    }

    public static void setDecoder(MessageDecoder decoder) {
        Utils.checkNull(decoder, "decoder");
        Config.decoder = decoder;
    }

    public static MessageEncoder getEncoder() {
        return encoder;
    }

    public static void setEncoder(MessageEncoder encoder) {
        Utils.checkNull(encoder, "encoder");
        Config.encoder = encoder;
    }

}
