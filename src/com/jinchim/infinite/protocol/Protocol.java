package com.jinchim.infinite.protocol;

import com.jinchim.infinite.Config;
import com.jinchim.infinite.Utils;

import java.nio.charset.Charset;

public final class Protocol {

    public static final short Method_Notify = 0;
    public static final short Method_Request = 1;
    public static final short Method_Response = 2;

    // 协议包的总大小（字节长度，包括自身的 4 个字节），占协议包的 0 ~ 3 字节
    private int length;
    // 协议包的请求方法，占协议包 4 ~ 5 字节
    private short method;
    // 协议包的路由信息的大小（字节长度，不包括自身的 2 个字节），占协议包的 6 ~ 7 字节
    private short routeLength;
    // 协议包的路由信息，占协议包的 8 ~ (routeLength + 7) 字节
    private String route;
    // 协议包的内容的大小（字节长度，不包括自身的 2 个字节），占协议包 (routeLength + 8) ~ (routeLength + 9) 字节
    private short contentLength;
    // 协议包的内容，占协议包 (routeLength + 10) ~ (routeLength + contentLength + 9) 字节
    private Message content;

    public void setMethod(short method) {
        this.method = method;
    }

    public void setRoute(String route) {
        Utils.checkNull(route, "route");
        byte[] bytes = route.getBytes(Charset.forName("UTF-8"));
        int length = bytes.length;
        if (length > Short.MAX_VALUE) {
            throw new RuntimeException("The length of the route cannot exceed " + Short.MAX_VALUE + " bytes.");
        }
        this.routeLength = (short) length;
        this.route = route;
    }

    public void setContent(Message content) {
        Utils.checkNull(content, "content");
        int length = content.length();
        if (length > Short.MAX_VALUE) {
            throw new RuntimeException("The length of the content cannot exceed " + Short.MAX_VALUE + " bytes.");
        }
        this.contentLength = (short) length;
        this.content = content;
    }

    public int getLength() {
        return 4 + 2 + 2 + routeLength + 2 + contentLength;
    }

    public short getMethod() {
        return method;
    }

    public short getRouteLength() {
        return routeLength;
    }

    public String getRoute() {
        return route;
    }

    public short getContentLength() {
        return contentLength;
    }

    public Message getContent() {
        return content;
    }

    public byte[] toByteArray() {
        Utils.checkNull(route, "route");
        Utils.checkNull(content, "content");
        byte[] length = Utils.intToByteArray(getLength()); // byte[4]
        byte[] method = Utils.shortToByteArray(getMethod()); // byte[2]
        byte[] routeLength = Utils.shortToByteArray(getRouteLength()); // byte[2]
        byte[] contentLength = Utils.shortToByteArray(getContentLength()); // byte[2]
        byte[] result = new byte[length.length + 1 + 1 + routeLength.length + contentLength.length + getRouteLength() + getContentLength()];
        // length
        result[0] = length[0];
        result[1] = length[1];
        result[2] = length[2];
        result[3] = length[3];
        // method
        result[4] = method[0];
        result[5] = method[1];
        // routeLength
        result[6] = routeLength[0];
        result[7] = routeLength[1];
        // route
        for (int i = 8; i < getRouteLength() + 8; i++) {
            byte[] bytes = route.getBytes(Charset.forName("UTF-8"));
            result[i] = bytes[i - 8];
        }
        // contentLength
        result[getRouteLength() + 8] = contentLength[0];
        result[getRouteLength() + 9] = contentLength[1];
        // content
        for (int i = getRouteLength() + 10; i < getRouteLength() + getContentLength() + 10; i++) {
            byte[] bytes = Config.getEncoder().encode(content);
            result[i] = bytes[i - (getRouteLength() + 10)];
        }
        return result;
    }

    public static Protocol parseFormByteArray(byte[] bytes) {
        Utils.checkNull(bytes, "bytes");
        // 解析字节数组
        int length = Utils.byteArrayToInt(Utils.cut(bytes, 0, 4));
        short method = Utils.byteArrayToShort(Utils.cut(bytes, 4, 2));
        short routeLength = Utils.byteArrayToShort(Utils.cut(bytes, 6, 2));
        String route = "";
        if (routeLength != 0) {
            route = new String(Utils.cut(bytes, 8, routeLength), Charset.forName("UTF-8"));
        }
        short contentLenth = Utils.byteArrayToShort(Utils.cut(bytes, routeLength + 8, 2));
        byte[] content = new byte[0];
        if (contentLenth != 0) {
            content = Utils.cut(bytes, routeLength + 10, contentLenth);
        }
        // 组装对象
        Protocol result = new Protocol();
        result.length = length;
        result.setMethod(method);
        result.routeLength = routeLength;
        result.setRoute(route);
        result.contentLength = contentLenth;
        result.setContent(Config.getDecoder().decode(content));
        return result;
    }

    @Override
    public String toString() {
        return "length: " + getLength() + "\n" +
                "method: " + getMethod() + "\n" +
                "routeLength: " + getRouteLength() + "\n" +
                "route: " + getRoute() + "\n" +
                "contentLength: " + getContentLength() + "\n" +
                "content: " + getContent().toString();
    }

}
