package com.jinchim.infinite.rpc;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;

public class RpcClient {

    public static <T> T getProxyObject(Class<?> rpcInterface, String ip, int port) {
        Object proxy = Proxy.newProxyInstance(rpcInterface.getClassLoader(), new Class[]{rpcInterface}, new RpcInvocationHandler(rpcInterface, ip, port));
        return (T) proxy;
    }

    private static class RpcInvocationHandler implements InvocationHandler {

        private Class<?> rpcInterface;
        private String ip;
        private int port;

        private RpcInvocationHandler(Class<?> rpcInterface, String ip, int port) {
            this.rpcInterface = rpcInterface;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
            Socket socket = null;
            ObjectOutputStream output = null;
            ObjectInputStream input = null;
            try {
                socket = new Socket(ip, port);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.writeUTF(rpcInterface.getName());
                output.writeUTF(method.getName());
                output.writeObject(method.getParameterTypes());
                output.writeObject(args);
                input = new ObjectInputStream(socket.getInputStream());
                return input.readObject();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }

    }

}
