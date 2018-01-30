package com.jinchim.infinite.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jinchim.infinite.protocol.Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InfiniteServerHelper {

    private final String classPath = getClass().getResource("/").getPath();
    private final String projectPath = new File("").getAbsolutePath();
    // 所有路由相关信息
    private Map<String, List<Map<Object, List<Method>>>> routeInfo;
    // 是否存在正确格式的 infinite-config.json 文件
    private boolean isInfiniteConfigCorrect;
    // 分布式服务的配置信息
    InfiniteConfigJson infiniteConfigJson;

    InfiniteServerHelper() {
        routeInfo = new HashMap<>();
    }

    void init() {
        // 搜索工程目录下的 infinite-config.json 文件
        findInfiniteConfigFile(projectPath);
        if (!isInfiniteConfigCorrect) {
            throw new RuntimeException("The infinite-config.json has some errors.");
        }
        // 搜索根目录下的所有带注解的 Class 文件
        findRouteClass(classPath);
    }

    private void findInfiniteConfigFile(String path) {
        File f = new File(path);
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findInfiniteConfigFile(file.getAbsolutePath());
            } else {
                if (file.getName().equals("infinite-config.json")) {
                    // 读取 infinite-config.json 的内容
                    readInfiniteConfigFile(file);
                    break;
                }
            }
        }
    }

    private void readInfiniteConfigFile(File file) {
        try {
            InputStream input = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            List<Byte> datas = new ArrayList<>();
            int length;
            while ((length = input.read(bytes)) != -1) {
                for (int i = 0; i < length; i++) {
                    datas.add(bytes[i]);
                }
            }
            byte[] result = new byte[datas.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = datas.get(i).byteValue();
            }
            String jsonStr = new String(result);
            // 解析 Json 字符串
            infiniteConfigJson = new Gson().fromJson(jsonStr, new TypeToken<InfiniteConfigJson>() {
            }.getType());
            if (infiniteConfigJson != null && infiniteConfigJson.master != null && infiniteConfigJson.master.ip != null && infiniteConfigJson.master.port != null) {
                isInfiniteConfigCorrect = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findRouteClass(String path) {
        File[] files = new File(path).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                findRouteClass(file.getAbsolutePath());
            } else {
                // 路径字符串的处理
                String fileName = file.getAbsolutePath().replace(new File(classPath).getAbsolutePath(), "");
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
                    // 找到正确的类，并找到带有 IMRoute 注解的类
                    Class<?> clazz = Class.forName(className);
                    Route annotation = clazz.getAnnotation(Route.class);
                    if (annotation != null) {
                        // 将相关信息保存，并做一些初始化操作
                        handlerRouteClass(clazz, annotation);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlerRouteClass(Class<?> clazz, Route annotation) {
        try {
            // 父类必须是 RouteHandler.class
            if (RouteHandler.class.isAssignableFrom(clazz)) {
                // 实例化自身
                Object object = clazz.newInstance();
                // 找到父类的所有方法
                Class<?> superClass = clazz.getSuperclass();
                Method[] methods = superClass.getDeclaredMethods();
                // 该路由对应的类的所有的方法集合（除 init() 外）
                List<Map<Object, List<Method>>> list = new ArrayList<>();
                if (routeInfo.get(annotation.value()) != null) {
                    list.addAll(routeInfo.get(annotation.value()));
                }
                Map<Object, List<Method>> map = new HashMap<>();
                List<Method> listMethod = new ArrayList<>();
                for (Method method : methods) {
                    // 调用 init() 方法
                    if ("init".equals(method.getName())) {
                        method.invoke(object);
                        continue;
                    }
                    listMethod.add(method);
                }
                map.put(object, listMethod);
                list.add(map);
                // 加入路由信息中
                routeInfo.put(annotation.value(), list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 解析客户端发送的协议包
    void parse(Protocol protocol, Session session) {
        List<Map<Object, List<Method>>> list = routeInfo.get(protocol.getRoute());
        if (list == null) {
            return;
        }
        // 解析路由信息中是否包含接收的路由信息
        for (Map<Object, List<Method>> map : list) {
            for (Object object : map.keySet()) {
                List<Method> methods = map.get(object);
                for (Method method : methods) {
                    parseMethod(object, method, protocol, session);
                }
            }
        }
    }

    private void parseMethod(Object object, Method method, Protocol protocol, Session session) {
        // 解析请求方法
        if (protocol.getMethod() == Protocol.Method_Notify) {
            if (method.getName().equals("doNotify")) {
                try {
                    method.invoke(object, protocol.getContent(), session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
