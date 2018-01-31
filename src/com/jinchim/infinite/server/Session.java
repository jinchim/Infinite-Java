package com.jinchim.infinite.server;

import com.jinchim.infinite.Utils;
import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.Protocol;

import io.netty.channel.Channel;

public final class Session {

    private String id;
    private Channel channel;

    Session(Channel channel) {
        this.channel = channel;
    }

    String getId() {
        return id;
    }

    void close() {
        try {
            channel.close().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bindId(String id) {
        Utils.checkNull(id, "id");
        for (Session session : InfiniteServer.getInstance().sessions) {
            if (session.id.equals(id)) {
                InfiniteServer.getInstance().sessions.remove(session);
                break;
            }
        }
        this.id = id;
        InfiniteServer.getInstance().sessions.add(this);
    }

    public void notify(String route, Message message) {
        Protocol protocol = new Protocol();
        protocol.setMethod(Protocol.Method_Notify);
        protocol.setRoute(route);
        protocol.setContent(message);
        channel.writeAndFlush(protocol);
    }

}
