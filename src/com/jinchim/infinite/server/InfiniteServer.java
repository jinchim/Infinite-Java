package com.jinchim.infinite.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jinchim.infinite.Config;
import com.jinchim.infinite.protocol.MessageDecoder;
import com.jinchim.infinite.protocol.MessageEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class InfiniteServer {

    private static final String TAG = "InfiniteServer";

    // class 文件的根目录
    private final String classRootPath = getClass().getResource("/").getPath();
    // 项目的根目录
    private final String projectRootPath = new File("").getAbsolutePath();
    // 是否存在正确格式的 infinite-config.json 文件
    private boolean isInfiniteConfigCorrect;
    // 分布式服务的配置信息
    private InfiniteConfigJson infiniteConfigJson;

    private static InfiniteServer instance;

    private InfiniteServer() {
    }

    public void init() {
        try {
            System.out.println(TAG + ": init start");
            // 搜索工程目录下的 infinite-config.json 文件
            findInfiniteConfigFile(projectRootPath);
            if (!isInfiniteConfigCorrect) {
                throw new RuntimeException("The infinite-config.json has some errors.");
            } else {
                // 如果 infinite-config.json 配置正确则启动 master 服务
                startMasterServer();
            }
            System.out.println(TAG + ": init success");
        } catch (Exception e) {
            System.out.println(TAG + ": init failed => " + e.getMessage());
        }
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
                "java -cp .:" + infiniteConfigJson.master.projectPath + projectName + infiniteConfigJson.project.libPath + "*" + " com.jinchim.infinite.server.master.MasterServer");
        sshHelper.release();
    }


//    public void pushMessage(String id, String route, Message message) {
//        Utils.checkNull(id, "id");
//        for (Session session : NettyServerHelper.sessions) {
//            if (id.equals(session.getId())) {
//                session.notify(route, message);
//                break;
//            }
//        }
//    }
//
//    public void pushMessageAll(String route, Message message) {
//        for (Session session : sessions) {
//            session.notify(route, message);
//        }
//    }

    public void release() {
        instance = null;
    }

    public static InfiniteServer getInstance() {
        if (instance == null) {
            synchronized (InfiniteServer.class) {
                if (instance == null) {
                    instance = new InfiniteServer();
                }
            }
        }
        return instance;
    }

    public InfiniteServer decoder(MessageDecoder decoder) {
        Config.setDecoder(decoder);
        return instance;
    }

    public InfiniteServer encoder(MessageEncoder encoder) {
        Config.setEncoder(encoder);
        return instance;
    }

}
