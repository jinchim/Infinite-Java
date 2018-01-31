package com.jinchim.infinite.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

final class InfiniteConfigJson {

    @SerializedName("master") MasterJson master;
    @SerializedName("distribution") List<DistributionJson> distribution;

    final static class MasterJson {

        @SerializedName("ip") String ip;
        @SerializedName("port") Integer port;
        @SerializedName("ssh_port") Integer sshPort;
        @SerializedName("username") String username;
        @SerializedName("password") String password;
        @SerializedName("project_path") String projectPath;

    }

    final static class DistributionJson {

        @SerializedName("name") String name;
        @SerializedName("config") List<ConfigJson> config;

        final static class ConfigJson {

            @SerializedName("id") Integer id;
            @SerializedName("ip") String ip;
            @SerializedName("port") Integer port;

        }

    }


    @Override
    public String toString() {
        // 这种创建方式可以把属性值为 null 的字段显示出来
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
