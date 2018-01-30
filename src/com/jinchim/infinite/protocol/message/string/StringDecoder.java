package com.jinchim.infinite.protocol.message.string;


import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.MessageDecoder;

import java.nio.charset.Charset;

public class StringDecoder extends MessageDecoder {

    @Override
    protected Message decode(byte[] bytes) {
        return new StringMessage(new String(bytes, Charset.forName("UTF-8")));
    }

}
