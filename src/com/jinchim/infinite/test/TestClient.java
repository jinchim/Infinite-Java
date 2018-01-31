package com.jinchim.infinite.test;


import com.jinchim.infinite.client.InfiniteClient;
import com.jinchim.infinite.protocol.message.string.StringMessage;

public class TestClient {

    public static void main(String[] args) {
        InfiniteClient.getInstance().connect("127.0.0.1", 9998);
        InfiniteClient.getInstance().notify("test1.TestHandler.x5", new StringMessage("xixi"));
    }

}
