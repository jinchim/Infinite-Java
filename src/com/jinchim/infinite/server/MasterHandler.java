package com.jinchim.infinite.server;

import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.message.string.StringMessage;

@Distribution("master")
final class MasterHandler {

    private void test(Message message, Session session) {
        StringMessage stringMessage = (StringMessage) message;
        session.notify("test", new StringMessage("回复你：" + stringMessage.getMessage()));
    }

}
