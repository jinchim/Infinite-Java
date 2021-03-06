package com.jinchim.infinite.client;

import com.jinchim.infinite.Config;
import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.MessageDecoder;
import com.jinchim.infinite.protocol.MessageEncoder;
import com.jinchim.infinite.protocol.Protocol;
import com.jinchim.infinite.protocol.ProtocolDecoder;
import com.jinchim.infinite.protocol.ProtocolEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jinchim.infinite.protocol.message.string.StringMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public final class InfiniteClient {

    private static final String TAG = "InfiniteClient";

    private static InfiniteClient instance;
    private EventLoopGroup group;
    private Channel channel;
    private boolean isConnected;
    // 监听服务器推送消息的集合
    private Map<String, List<OnMessageListener>> listeners;

    private InfiniteClient() {
        group = new NioEventLoopGroup();
        listeners = new HashMap<>();
    }

    public boolean connect(String ip, int port) {
        try {
            System.out.println(TAG + ": connect start => " + ip + ":" + port);
            Bootstrap bootstrap = new Bootstrap();
            ChannelFuture channelFuture = bootstrap
                    .group(group) // 绑定线程池
                    .channel(NioSocketChannel.class) // 指定使用异步处理事件的 channel
                    .handler(new InitHandler()) // 客户端初始化操作
                    .connect(ip, port) // 连接服务器
                    .sync(); // 同步操作
            // 获取用于通讯的 channel
            channel = channelFuture.channel();
            // 连接成功
            System.out.println(TAG + ": connect success => " + ip + ":" + port);
            isConnected = true;
        } catch (Exception e) {
            System.out.println(TAG + ": connect failed => " + e.getMessage());
            try {
                group.shutdownGracefully().sync();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            isConnected = false;
        }
        return isConnected;
    }

    private class InitHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline()
                    // 自定义协议的解码器
                    .addLast(new ProtocolDecoder())
                    // 自定义协议的编码器
                    .addLast(new ProtocolEncoder())
                    // 逻辑处理
                    .addLast(new ClientHandler());
        }
    }

    private class ClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Protocol protocol = (Protocol) msg;
            System.out.println(TAG + ": receive success => " + ctx.channel().remoteAddress() + ", msg => " + protocol.toString());
            // 解析服务器发送的数据
            if (protocol.getMethod() == Protocol.Method_Notify) {
                for (String key : listeners.keySet()) {
                    for (OnMessageListener listener : listeners.get(key)) {
                        if (key.equals(protocol.getRoute())) {
                            listener.onMessage(protocol.getContent());
                        }
                    }
                }
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println(TAG + ": receive error => " + cause.getMessage());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println(TAG + ": server disconnect => " + ctx.channel().remoteAddress());
            // 断开连接
            ctx.close().sync();
        }

    }

    public boolean isConnected() {
        return isConnected;
    }

    public void on(String route, OnMessageListener listener) {
        if (listener != null) {
            List<OnMessageListener> temp = new ArrayList<>();
            if (listeners.get(route) != null) {
                temp.addAll(listeners.get(route));
            }
            temp.add(listener);
            listeners.put(route, temp);
        }
    }

    public void notify(String route, String stringMessage) {
        Protocol protocol = new Protocol();
        protocol.setMethod(Protocol.Method_Notify);
        protocol.setRoute(route);
        protocol.setContent(new StringMessage(stringMessage));
        send(protocol);
    }

    public void notify(String route, Message message) {
        Protocol protocol = new Protocol();
        protocol.setMethod(Protocol.Method_Notify);
        protocol.setRoute(route);
        protocol.setContent(message);
        send(protocol);
    }

    private void send(Protocol protocol) {
        if (channel == null) {
            System.out.println(TAG + ": socket init failed");
            return;
        }
        try {
            channel.writeAndFlush(protocol).sync();
            System.out.println(TAG + ": send success");
        } catch (Exception e) {
            System.out.println(TAG + ": send failed => " + e.getMessage());
        }
    }

    public void release() {
        try {
            if (group != null) {
                group.shutdownGracefully().sync();
            }
            if (channel != null) {
                channel.close().sync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isConnected = false;
        listeners.clear();
        group = null;
        channel = null;
        listeners = null;
        instance = null;
    }

    public static InfiniteClient getInstance() {
        if (instance == null) {
            synchronized (InfiniteClient.class) {
                if (instance == null) {
                    instance = new InfiniteClient();
                }
            }
        }
        return instance;
    }

    public InfiniteClient decoder(MessageDecoder decoder) {
        Config.setDecoder(decoder);
        return instance;
    }

    public InfiniteClient encoder(MessageEncoder encoder) {
        Config.setEncoder(encoder);
        return instance;
    }

}
