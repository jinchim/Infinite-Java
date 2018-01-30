package com.jinchim.infinite.protocol.message.string;

import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.MessageEncoder;

import java.nio.charset.Charset;


public class StringEncoder extends MessageEncoder {

    @Override
    protected byte[] encode(Message message) {
        StringMessage stringMessage = (StringMessage) message;
        return stringMessage.getMessage().getBytes(Charset.forName("UTF-8"));
    }

}
