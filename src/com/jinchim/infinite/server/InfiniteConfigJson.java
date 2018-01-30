package com.jinchim.infinite.server;

import com.google.gson.annotations.SerializedName;

import java.util.List;

final class InfiniteConfigJson {

    @SerializedName("master")
    MasterJson master;
    @SerializedName("handler")
    List<HandlerJson> handler;

    final static class MasterJson {

        @SerializedName("ip")
        String ip;
        @SerializedName("port")
        Integer port;

    }

    final static class HandlerJson {

        @SerializedName("name")
        String name;
        @SerializedName("config")
        List<ConfigJson> config;

        final static class ConfigJson {

            @SerializedName("id")
            Integer id;
            @SerializedName("ip")
            String ip;
            @SerializedName("port")
            Integer port;

        }

    }

}
