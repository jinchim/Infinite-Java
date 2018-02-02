package com.jinchim.infinite.server;

import com.jinchim.infinite.protocol.Protocol;
import com.jinchim.infinite.protocol.ProtocolDecoder;
import com.jinchim.infinite.protocol.ProtocolEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.List;

final class NettyServerHelper {

    private static final String TAG = "NettyServerHelper";

    // 用于分配处理业务线程的线程组
    private EventLoopGroup baseGroup;
    // 业务处理线程的线程组
    private EventLoopGroup workerGroup;
    // 管理客户端连接，必须调用 Session 的对象方法 bindId() 才会加入集合
    private List<Session> sessions;

    NettyServerHelper() {
        baseGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        sessions = new ArrayList<>();
    }

    void init(int port) {
        try {
            System.out.println(TAG + ": init start");
            // 启动分布式服务
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(baseGroup, workerGroup) // 绑定线程池
                    .option(ChannelOption.SO_KEEPALIVE, true) // 保持长连接
                    .channel(NioServerSocketChannel.class) // 指定使用异步处理事件的 channel
                    .childHandler(new InitHandler()) // 绑定客户端连接时候触发的操作
                    .bind(port) // 绑定端口
                    .sync(); // 同步操作
            System.out.println(TAG + ": init success, listen on port => " + port);
        } catch (Exception e) {
            System.out.println(TAG + ": init falied => " + e.getMessage());
        }
    }

    private class InitHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            // 有客户端连接
            System.out.println(TAG + ": client connect => " + channel.remoteAddress());
            channel.pipeline()
                    // 自定义协议的解码器
                    .addLast(new ProtocolDecoder())
                    // 自定义协议的编码器
                    .addLast(new ProtocolEncoder())
                    // 逻辑处理
                    .addLast(new ServerHandler());
        }
    }

    private class ServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Protocol protocol = (Protocol) msg;
            System.out.println(TAG + ": receive success => " + ctx.channel().remoteAddress() + ", msg => " + protocol.toString());
            // 解析客户端发送的数据
//            helper.parse(protocol, new Session(ctx.channel()));
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
            System.out.println(TAG + ": client disconnect => " + ctx.channel().remoteAddress());
            // 客户端断开连接
            ctx.close().sync();
        }
    }

    void release() {
        try {
            if (baseGroup != null) {
                baseGroup.shutdownGracefully().sync();
                baseGroup = null;
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
                workerGroup = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Session session : sessions) {
            session.close();
        }
        sessions.clear();
        sessions = null;
    }

}
