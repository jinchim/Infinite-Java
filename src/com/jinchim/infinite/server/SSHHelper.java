package com.jinchim.infinite.server;


import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.InputStream;

final class SSHHelper {

    private static final String TAG = "SSHHelper";

    private String ip;
    private int port;
    private String username;
    private String password;

    private Session session;
    private ChannelSftp channelSftp;
    private boolean isConnected;

    SSHHelper(String ip, int port, String username, String password) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    boolean connect() {
        try {
            // 连接 SSH
            System.out.println(TAG + ": connect start => " + ip + ":" + port);
            JSch jSch = new JSch();
            session = jSch.getSession(username, ip, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(15 * 1000);
            isConnected = true;
            // 打开 sftp 通道并连接
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            System.out.println(TAG + ": connect success => " + ip + ":" + port);
        } catch (Exception e) {
            System.out.println(TAG + ": connect failed => " + e.getMessage());
            isConnected = false;
        }
        return isConnected;
    }

    void uploadFile(String src, String dst) {
        // 创建远程服务器接收文件的目录
        execute("mkdir -p " + dst);
        // 递归上传文件
        File file = new File(src);
        copyFile(file, dst);
    }

    private void copyFile(File file, String dst) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            try {
                channelSftp.mkdir(dst + file.getName());
            } catch (Exception e) {
            }
            try {
                channelSftp.cd(file.getName());
            } catch (Exception e) {
            }
            dst = dst + file.getName() + "/";
            for (int i = 0; i < files.length; i++) {
                copyFile(files[i], dst);
            }
        } else {
            try {
                System.out.println(TAG + ": uploadFile => " + ip + ":" + port + dst + file.getName());
                channelSftp.put(file.getAbsolutePath(), dst);
                System.out.println(TAG + ": uploadFile success");
            } catch (SftpException e) {
                System.out.println(TAG + ": uploadFile failed => " + e.getMessage());
            }
        }
    }

    void execute(String cmd) {
        ChannelExec channelExec = null;
        try {
            // 执行 Linux 命令
            System.out.println(TAG + ": execute => " + ip + ":" + port + " " + cmd);
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(cmd);
            channelExec.setInputStream(null);
            channelExec.setErrStream(System.err);
            channelExec.connect();
            InputStream in = channelExec.getInputStream();
            InputStream error = channelExec.getErrStream();
            byte[] tmp = new byte[1024];
            byte[] tmpError = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    System.out.print(new String(tmp, 0, i));
                }
                while (error.available() > 0) {
                    int i = error.read(tmpError, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    System.out.print(new String(tmpError, 0, i));
                }
                if (channelExec.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    System.out.println(TAG + ": executeExitStatus => " + channelExec.getExitStatus());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(TAG + ": execute failed => " + e.getMessage());
        } finally {
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }
    }

    void release() {
        if (channelSftp != null) {
            channelSftp.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
        isConnected = false;
        channelSftp = null;
        session = null;
    }

}
