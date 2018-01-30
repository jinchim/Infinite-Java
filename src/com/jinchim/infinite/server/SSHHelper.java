package com.jinchim.infinite.server;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

final class SSHHelper {

    private static final String TAG = "SSHHelper";

    private Session session;
    private Channel channel;
    private boolean isConnected;

    SSHHelper() {
    }

    boolean connect(String ip, int port, String username, String password) {
        try {
            JSch jSch = new JSch();
            session = jSch.getSession(username, ip, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            isConnected = true;
            System.out.println(TAG + ": connect success => " + ip + ", " + port);
        } catch (Exception e) {
            System.out.println(TAG + ": connect failed => " + e.getMessage());
            isConnected = false;
        }
        return isConnected;
    }

    void exec(String cmd) {
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            channel.connect();
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
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    System.out.println(TAG + ": exitStatus => " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void release() {
        if (channel != null) {
            channel.disconnect();
            channel = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
        isConnected = false;
    }

}
