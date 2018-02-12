package com.jinchim.infinite.server;


final class StartMasterServer {

    public static void main(String[] args) {
        // 前五个参数分别为：ip 地址、监听端口、项目的名称、项目的 .jar 文件路径、项目的资源文件路径
        // 后面如果还有参数肯定是 master 服务要启动的其他分布式服务的参数
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String projectName = args[2];
        String libPath = args[3];
        String resPath = args[4];

        // 启动 master 服务
        InfiniteServerHelper helper = new InfiniteServerHelper("master");
        helper.startServer(port);

        // master 启动成功后则启动其他分布式服务
        for (int i = 5; i < args.length; i += 7) {
            String serverName = args[i];
            String distributionIp = args[i + 1];
            int distributionPort = Integer.parseInt(args[i + 2]);
            int sshPort = Integer.parseInt(args[i + 3]);
            String username = args[i + 4];
            String password = args[i + 5];
            String projectPath = args[i + 6];

            // 连接服务器并启动服务
            new Thread(() -> {
                SSHHelper sshHelper = new SSHHelper(distributionIp, sshPort, username, password);
                sshHelper.connect();
                StringBuilder execute = new StringBuilder();
                execute.append("cd " + projectPath + projectName + "/classes/;" +
                        "chmod u+x " + projectPath + projectName + libPath + "*.jar;" +
                        "java -cp .:" + projectPath + projectName + libPath + "*" + " com.jinchim.infinite.server.StartDistributionServer " + serverName + " " + distributionPort + " " + projectName + " " + libPath + " " + resPath + " " + ip + " " + port);
                sshHelper.execute(execute.toString());
                sshHelper.release();
            }).start();
        }
    }

}
