package com.jinchim.infinite.test;


import com.jinchim.infinite.client.InfiniteClient;
import com.jinchim.infinite.client.OnMessageListener;
import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.message.string.StringMessage;

public class TestClient {

    public static void main(String[] args) {
        InfiniteClient.getInstance().connect("47.97.171.3", 9999);
        InfiniteClient.getInstance().notify("master.MasterHandler.test", new StringMessage("jinzi"));
        InfiniteClient.getInstance().on("test", message -> {
            StringMessage stringMessage = (StringMessage) message;
            System.out.println(stringMessage.getMessage());
        });

    }

}
