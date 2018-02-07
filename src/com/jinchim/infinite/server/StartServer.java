package com.jinchim.infinite.server;


final class StartServer {

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new RuntimeException("The args should not be less than 2.");
        }
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);

        InfiniteServerHelper helper = new InfiniteServerHelper();
        helper.startServer(serverName, port);
    }

}
