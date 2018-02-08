package com.jinchim.infinite.test;

import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.message.string.StringMessage;
import com.jinchim.infinite.server.Distribution;
import com.jinchim.infinite.server.InfiniteServer;
import com.jinchim.infinite.server.Session;

@Distribution("test")
public class TestHandler {

    public void x1(Message message, Session session) {
        StringMessage stringMessage = (StringMessage) message;
        System.out.println("x1 => " + stringMessage.getMessage());
    }

    protected void x2(Message message, Session session) {
        StringMessage stringMessage = (StringMessage) message;
        System.out.println("x2 => " + stringMessage.getMessage());
    }

    void x3(Message message) {
        StringMessage stringMessage = (StringMessage) message;
        System.out.println("x3 => " + stringMessage.getMessage());
    }

    private void x4(Session session, Message message) {
        StringMessage stringMessage = (StringMessage) message;
        System.out.println("x4 => " + stringMessage.getMessage());
    }

    private void x5() {
        System.out.println("x5 => ");
    }

}
