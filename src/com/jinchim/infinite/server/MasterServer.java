package com.jinchim.infinite.server;

import com.jinchim.infinite.protocol.Message;
import com.jinchim.infinite.protocol.Protocol;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class MasterServer {

    private static final String TAG = "MasterServer";

    private final String classRootPath = getClass().getResource("/").getPath();
    // 带有注解的类的方法信息
    private Map<String, Map<Object, Method>> annotationMethodInfo;

    MasterServer() {
        annotationMethodInfo = new HashMap<>();
    }

    void init() {
        // 搜索所有带 @Distribution 注解的 Class 文件
        findAnnotationClass(classRootPath);
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
                    // 处理带有 @Distribution 注解的类
                    Distribution annotation = clazz.getAnnotation(Distribution.class);
                    if (annotation != null) {
                        if (annotation.value().equals("master")) {
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
            System.out.println(clazz.getSimpleName() + " => " + annotation.value());
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

    private static MasterServer masterServer;

    public static void main(String[] args) {
        masterServer = new MasterServer();
        masterServer.init();
    }


}
