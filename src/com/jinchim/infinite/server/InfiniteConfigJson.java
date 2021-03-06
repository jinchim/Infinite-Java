package com.jinchim.infinite.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

final class InfiniteConfigJson {

    @SerializedName("master") ConfigJosn master;
    @SerializedName("distribution") List<DistributionJson> distribution;
    @SerializedName("project") ProjectJson project;

    final static class ConfigJosn {

        @SerializedName("ip") String ip;
        @SerializedName("port") Integer port;
        @SerializedName("ssh_port") Integer sshPort;
        @SerializedName("username") String username;
        @SerializedName("password") String password;
        @SerializedName("project_path") String projectPath;

    }

    final static class DistributionJson {

        @SerializedName("name") String name;
        @SerializedName("config") List<ConfigJosn> config;

    }

    final static class ProjectJson {

        @SerializedName("name") String name;
        @SerializedName("lib_path") String libPath;
        @SerializedName("res_path") String resPath;

    }

    @Override
    public String toString() {
        // 这种创建方式可以把属性值为 null 的字段显示出来
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
