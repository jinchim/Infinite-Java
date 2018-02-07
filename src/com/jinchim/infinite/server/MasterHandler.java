package com.jinchim.infinite.server;

import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.message.string.StringMessage;

@Distribution("master")
final class MasterHandler {

    private void xixi(Message message, Session session) {
        StringMessage stringMessage = (StringMessage) message;
        System.out.println("xixi => " + stringMessage.getMessage());
    }

}
