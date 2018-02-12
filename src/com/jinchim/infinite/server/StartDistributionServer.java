package com.jinchim.infinite.server;


import com.jinchim.infinite.client.InfiniteClient;

final class StartDistributionServer {

    public static void main(String[] args) {
        // 前七个参数分别为：服务名、监听端口、项目的名称、项目的 .jar 文件路径、项目的资源文件路径、master 服务器的 ip 地址、master 服务器的监听端口
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        String projectName = args[2];
        String libPath = args[3];
        String resPath = args[4];
        String masterIp = args[5];
        int masterPort = Integer.parseInt(args[6]);

        // 启动分布式服务
        InfiniteServerHelper helper = new InfiniteServerHelper(serverName);
        helper.startServer(port);

        // 如果服务启动成功则连接到 master 服务器
        InfiniteClient.getInstance().connect(masterIp, masterPort);

    }

}
