package com.jinchim.infinite.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jinchim.infinite.protocol.Message;
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

    private final String classRootPath = getClass().getResource("/").getPath();
    private final String projectRootPath = new File("").getAbsolutePath();
    // 带有注解的类的方法信息
    private Map<String, Map<Object, Method>> annotationMethodInfo;
    // 是否存在正确格式的 infinite-config.json 文件
    private boolean isInfiniteConfigCorrect;
    // 分布式服务的配置信息
    private InfiniteConfigJson infiniteConfigJson;

    InfiniteServerHelper() {
        annotationMethodInfo = new HashMap<>();
    }

    void init() {
        // 搜索工程目录下的 infinite-config.json 文件
        findInfiniteConfigFile(projectRootPath);
        if (!isInfiniteConfigCorrect) {
            throw new RuntimeException("The infinite-config.json has some errors.");
        } else {
            // 如果 infinite-config.json 配置正确则启动 master 服务
            startMasterServer();
        }
        // 搜索所有带 @Distribution 注解的 Class 文件
        findAnnotationClass(classRootPath);
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
            if (infiniteConfigJson != null &&
                    infiniteConfigJson.master != null &&
                    infiniteConfigJson.master.ip != null &&
                    infiniteConfigJson.master.rpcPort != null &&
                    infiniteConfigJson.master.sshPort != null &&
                    infiniteConfigJson.master.username != null &&
                    infiniteConfigJson.master.password != null &&
                    infiniteConfigJson.master.projectPath != null &&
                    infiniteConfigJson.project != null &&
                    infiniteConfigJson.project.libPath != null &&
                    infiniteConfigJson.project.resPath != null) {
                isInfiniteConfigCorrect = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startMasterServer() {
        SSHHelper sshHelper = new SSHHelper(infiniteConfigJson.master.ip, infiniteConfigJson.master.sshPort, infiniteConfigJson.master.username, infiniteConfigJson.master.password);
        sshHelper.connect();
        String projectName = new File(projectRootPath).getName();
        File[] files = new File(classRootPath).listFiles();
        for (File file : files) {
            sshHelper.uploadFile(file.getAbsolutePath(), infiniteConfigJson.master.projectPath + projectName + "/classes/");
        }
        sshHelper.uploadFile(projectRootPath + infiniteConfigJson.project.libPath, infiniteConfigJson.master.projectPath + projectName + "/");
        sshHelper.uploadFile(projectRootPath + infiniteConfigJson.project.resPath, infiniteConfigJson.master.projectPath + projectName + "/");
        sshHelper.exec("cd " + infiniteConfigJson.master.projectPath + projectName + "/classes/;" +
                "chmod u+x " + infiniteConfigJson.master.projectPath + projectName + infiniteConfigJson.project.libPath + "*.jar;" +
                "java -cp .:" + infiniteConfigJson.master.projectPath + projectName + infiniteConfigJson.project.libPath + "*" + " com.jinchim.infinite.server.MasterServer");
        sshHelper.release();
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
                    Distribution annotation = clazz.getAnnotation(Distribution.class);
                    if (annotation != null) {
                        // 处理带有 Distribution 注解的类
                        handlerAnnotationClass(clazz, annotation);
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

    // 解析客户端发送的协议包
    void parse(Protocol protocol, Session session) {
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


}
