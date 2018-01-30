package com.jinchim.infinite;


public class Utils {

    /**
     * int 转化为 byte[4]
     */
    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /**
     * byte[4] 转化为 int
     */
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    /**
     * short 转化为 byte[2]
     */
    public static byte[] shortToByteArray(short a) {
        return new byte[]{
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /**
     * byte[2] 转化为 short
     */
    public static short byteArrayToShort(byte[] b) {
        return (short) ((b[1] & 0xFF) |
                (b[0] & 0xFF) << 8);
    }

    /**
     * 截取 byte[] 中的某一段 byte[]
     */
    public static byte[] cut(byte[] bytes, int start, int length) {
        if (start < 0) {
            throw new RuntimeException("Start can not less 0.");
        }
        if (start >= bytes.length) {
            throw new RuntimeException("Start can not exceed byte length.");
        }
        if (length < 0) {
            throw new RuntimeException("Length can not less 0.");
        }
        if (length > bytes.length - start) {
            throw new RuntimeException("Length can not exceed byte length.");
        }
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = bytes[start + i];
        }
        return result;
    }

    /**
     * 检查字段是否为 null
     */
    public static void checkNull(Object object, String msg) {
        if (object == null) {
            throw new RuntimeException("The " + msg + " cannot be null.");
        }
    }

}
