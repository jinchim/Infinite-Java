package com.jinchim.infinite.server;

import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.Protocol;
import com.jinchim.infinite.protocol.ProtocolDecoder;
import com.jinchim.infinite.protocol.ProtocolEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InfiniteServerHelper {

    private static String TAG = "InfiniteServerHelper";

    // Class 文件所在的根目录
    private final String classRootPath = getClass().getResource("/").getPath();
    // 带有注解的类（处理客户端发送协议包的类）的方法信息
    private Map<String, Map<Object, Method>> annotationMethodInfo;
    // 用于分配处理业务线程的线程组
    private EventLoopGroup baseGroup;
    // 业务处理线程的线程组
    private EventLoopGroup workerGroup;
    // 管理客户端连接，必须调用 Session 的对象方法 bindId() 才会加入集合
    private List<Session> sessions;
    // 服务名称，在 infinite-config.json 中指定
    private String serverName;

    InfiniteServerHelper(String serverName) {
        annotationMethodInfo = new HashMap<>();
        baseGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        sessions = new ArrayList<>();
        this.serverName = serverName;
        TAG += "(" + serverName + ")";
    }

    void startServer(int port) {
        // 搜索所有带 @Distribution 注解的 Class 文件，并将其信息保存
        findAnnotationClass(classRootPath);
        // 启动服务监听端口
        listen(port);
    }

    private void findAnnotationClass(String path) {
        File[] files = new File(path).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findAnnotationClass(file.getAbsolutePath());
            } else {
                // 路径字符串的处理
                String fileName = file.getAbsolutePath().replace(new File(classRootPath).getAbsolutePath(), "");
                String className = fileName;
                className = className.replace("/", ".");
                className = className.replace("\\", ".");
                if (className.startsWith(".")) {
                    className = className.substring(1, className.length());
                }
                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6);
                }
                try {
                    // 找到正确的类，并找到带有 Distribution 注解的类
                    Class<?> clazz = Class.forName(className);
                    // 处理带有 @Distribution 注解的类，并且区分注解值对应的服务
                    Distribution annotation = clazz.getAnnotation(Distribution.class);
                    if (annotation != null) {
                        if (serverName.equals(annotation.value())) {
                            handlerAnnotationClass(clazz, annotation);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlerAnnotationClass(Class<?> clazz, Distribution annotation) {
        try {
            // 实例化自身
            Object object = clazz.newInstance();
            // 找到自身所有的方法
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                // 筛选方法参数为 Message 和 Session 的的方法
                if (method.getParameterTypes().length == 2 && method.getParameterTypes()[0].equals(Message.class) && method.getParameterTypes()[1].equals(Session.class)) {
                    // 组装路由信息
                    String route = annotation.value() + "." + clazz.getSimpleName() + "." + method.getName();
                    Map<Object, Method> map = new HashMap<>();
                    map.put(object, method);
                    // 加入路由信息中
                    annotationMethodInfo.put(route, map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listen(int port) {
        try {
            System.out.println(TAG + ": init start");
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(baseGroup, workerGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class) // 指定使用异步处理事件的 channel
                    .childHandler(new InitHandler()) // 绑定客户端连接时候触发的操作
                    .bind(port) // 绑定端口
                    .sync(); // 同步操作
            System.out.println(TAG + ": init success, listen port => " + port);
        } catch (Exception e) {
            System.out.println(TAG + ": init failed => " + e.getMessage());
            try {
                baseGroup.shutdownGracefully().sync();
                workerGroup.shutdownGracefully().sync();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            System.exit(1);
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
            // 解析客户端发送的协议数据包
            parseProtocol(protocol, new Session(ctx.channel()));
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

    private void parseProtocol(Protocol protocol, Session session) {
        Map<Object, Method> map = annotationMethodInfo.get(protocol.getRoute());
        if (map == null) {
            return;
        }
        // 根据路由信息调用对应的方法
        for (Object object : map.keySet()) {
            Method method = map.get(object);
            if (protocol.getMethod() == Protocol.Method_Notify) {
                try {
                    method.setAccessible(true);
                    method.invoke(object, protocol.getContent(), session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void release() {
        try {
            if (baseGroup != null) {
                baseGroup.shutdownGracefully().sync();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Session session : sessions) {
            session.close();
        }
        sessions.clear();
        baseGroup = null;
        workerGroup = null;
        sessions = null;
    }


}
