package com.jinchim.infinite.test;

import com.jinchim.infinite.server.InfiniteServer;

public class TestServer {

    public static void main(String[] args) {
        InfiniteServer.getInstance().init();
    }


//    private static void printLines(String cmd, InputStream ins) throws Exception {
//        String line;
//        BufferedReader in = new BufferedReader(new InputStreamReader(ins, "GBK"));
//        while ((line = in.readLine()) != null) {
//            System.out.println(cmd + line);
//        }
//    }
//
//    private static void runProcess(String command) throws Exception {
//        Process process = Runtime.getRuntime().exec(command);
//        printLines(command + " (stdout) => ", process.getInputStream());
//        printLines(command + " (stderr) => ", process.getErrorStream());
//        process.waitFor();
//        System.out.println(command + " exitValue() " + process.exitValue());
//    }

}
