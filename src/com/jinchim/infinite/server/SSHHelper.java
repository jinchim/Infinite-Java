package com.jinchim.infinite.server;


import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.InputStream;
import java.util.Vector;

final class SSHHelper {

    private static final String TAG = "SSHHelper";


    private Session session;
    private ChannelExec channelExec;
    private ChannelSftp channelSftp;
    private boolean isConnected;

    SSHHelper() {
    }

    boolean connect(String ip, int port, String username, String password) {
        try {
            // 连接 SSH
            JSch jSch = new JSch();
            session = jSch.getSession(username, ip, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            isConnected = true;
            System.out.println(TAG + ": connect success => " + ip + ":" + port);
        } catch (Exception e) {
            System.out.println(TAG + ": connect failed => " + e.getMessage());
            isConnected = false;
        }
        return isConnected;
    }

    private void exec(String cmd) {
        try {
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(cmd);
            channelExec.setInputStream(null);
            channelExec.setErrStream(System.err);
            InputStream in = channelExec.getInputStream();
            channelExec.connect();
            System.out.println(TAG + ": exec => " + cmd);
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    System.out.print(new String(tmp, 0, i));
                }
                if (channelExec.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    System.out.println(TAG + ": exitStatus => " + channelExec.getExitStatus());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(TAG + ": exec failed => " + e.getMessage());
        } finally {
            channelExec.disconnect();
        }
    }

    private void uploadFile(String src, String dst) {
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.put(src, dst);
            System.out.println(TAG + ": uploadFile success");
        } catch (Exception e) {
            System.out.println(TAG + ": uploadFile failed => " + e.getMessage());
        } finally {
            channelSftp.disconnect();
        }
    }

    void uploadDir(String src, String dst) {
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.cd(dst);
        } catch (Exception e) {
            System.out.println(TAG + ": uploadDir failed => " + e.getMessage());
//            throw new RuntimeException("test");
        } finally {
            channelSftp.disconnect();
        }
//        findFile(src, dst);
    }

    private void findFile(String path, String dst) {
        File[] files = new File(path).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findFile(file.getAbsolutePath(), dst);
            } else {
                System.out.println(file.getAbsolutePath() + " => " + dst);
            }
        }
    }

    void release() {
        if (session != null) {
            session.disconnect();
        }
        channelExec = null;
        channelSftp = null;
        session = null;
        isConnected = false;
    }

}
