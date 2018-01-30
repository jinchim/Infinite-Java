package com.jinchim.infinite.server;

import com.jinchim.infinite.protocol.Message;

public abstract class RouteHandler {

    protected void init() {
    }

    protected void doNotify(Message message, Session session) {
    }

    protected void doReqeust(Message message, Session session) {
    }

    protected void destroy() {

    }

}
