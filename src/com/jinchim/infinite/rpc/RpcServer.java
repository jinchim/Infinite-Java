package com.jinchim.infinite.rpc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class RpcServer {

    private static final String TAG = "RpcServer";

    private static RpcServer instance;
    private Map<String, Class<?>> rpcInfo;

    private RpcServer() {
        rpcInfo = new HashMap<>();
    }

    public void init(int port) {
        try {
            // 监听端口
            System.out.println(TAG + ": init start");
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println(TAG + ": init success, listen on port => " + port);
            // 有客户端连接则进行相关处理
            Socket socket = serverSocket.accept();
            handler(socket);
        } catch (Exception e) {
            System.out.println(TAG + ": init falied => " + e.getMessage());
        }
    }

    private void handler(Socket socket) {
        ObjectInputStream input = null;
        ObjectOutputStream output = null;
        try {
            input = new ObjectInputStream(socket.getInputStream());
            String serviceName = input.readUTF();
            String methodName = input.readUTF();
            Class<?>[] parameterTypes = (Class<?>[]) input.readObject();
            Object[] arguments = (Object[]) input.readObject();
            Class serviceClass = rpcInfo.get(serviceName);
            if (serviceClass == null) {
                throw new ClassNotFoundException(serviceName + " not found");
            }
            Method method = serviceClass.getMethod(methodName, parameterTypes);
            Object result = method.invoke(serviceClass.newInstance(), arguments);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void register(Class<?> rpcInterface, Class<?> rpcInterfaceImpl) {
        rpcInfo.put(rpcInterface.getName(), rpcInterfaceImpl);
    }

    public static RpcServer getInstance() {
        if (instance == null) {
            synchronized (RpcServer.class) {
                if (instance == null) {
                    instance = new RpcServer();
                }
            }
        }
        return instance;
    }


}
