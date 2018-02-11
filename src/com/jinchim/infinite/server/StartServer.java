package com.jinchim.infinite.server;


import java.io.File;

final class StartServer {

    private StartServer(String serverName, int port, String projectName, String libPath, String resPath) {

    }

    public static void main(String[] args) {
        // 参数的前五个参数分别为：服务名、监听端口、项目的名称、项目的 .jar 文件路径、项目的资源文件路径
        // 后面如果还有参数肯定是 master 服务要启动的其他分布式服务的参数
//        new StartServer(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4]);

        // 启动监听服务
        InfiniteServerHelper helper = new InfiniteServerHelper(args[0]);
        helper.startServer(Integer.parseInt(args[1]));

        // 如果 master 服务启动成功，则启动其他分布式服务
        if ("master".equals(args[0])) {
            for (int i = 5; i < args.length; i += 7) {
                String serverName = args[i];
                String ip = args[i + 1];
                int port = Integer.parseInt(args[i + 2]);
                int sshPort = Integer.parseInt(args[i + 3]);
                String username = args[i + 4];
                String password = args[i + 5];
                String projectPath = args[i + 6];

                // 连接服务器并启动服务
                SSHHelper sshHelper = new SSHHelper(ip, sshPort, username, password);
                sshHelper.connect();
                StringBuilder execute = new StringBuilder();
                execute.append("cd " + projectPath + args[2] + "/classes/;" +
                        "chmod u+x " + projectPath + args[2] + args[3] + "*.jar;" +
                        "java -cp .:" + projectPath + args[2] + args[3] + "*" + " com.jinchim.infinite.server.StartServer " + serverName + " " + port + " " + args[2] + " " + args[3] + " " + args[4]);
                sshHelper.execute(execute.toString(), true);
                sshHelper.release();
            }
        }
    }

}
