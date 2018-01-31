package com.jinchim.infinite.server;

final class MasterServer {

    private static final String TAG = "MasterServer";

    private final String classRootPath = getClass().getResource("/").getPath();

    MasterServer() {
    }

    void init(String ip, int port, String username, String password, String projectPath) {
        SSHHelper sshHelper = null;
        sshHelper = new SSHHelper();
        sshHelper.connect(ip, port, username, password);
        sshHelper.uploadDir(classRootPath, projectPath);
        sshHelper.release();

    }


}
