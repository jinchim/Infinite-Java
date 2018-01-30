package com.jinchim.infinite.protocol.message.string;


import com.jinchim.infinite.protocol.Message;

import java.nio.charset.Charset;

public class StringMessage extends Message {

    private String message;

    public StringMessage(String message) {
        this.message = message;
    }

    @Override
    protected int length() {
        return message.getBytes(Charset.forName("UTF-8")).length;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }

}
