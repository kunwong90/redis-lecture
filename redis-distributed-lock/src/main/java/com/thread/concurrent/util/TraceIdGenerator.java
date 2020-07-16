package com.thread.concurrent.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wangkun2
 * 产生规则是:服务器 IP + 产生 ID 时候的时间 + 自增序列 + 当前进程号,比如:
 * <p>
 * 0ad1348f1403169275002100356696
 * <p>
 * 前 8 位 0ad1348f 即产生 TraceId 的机器的 IP,这是一个十六进制的数字,每两位代表 IP 中的一段,
 * 我们把这个数字,按每两位转成 10 进制即可得到常见的 IP 地址表示方式 10.209.52.143,
 * 大家也可以根据这个规律来查找到请求经过的第一个服务器.
 * 后面的 13 位 1403169275002 是产生 TraceId 的时间.之后的 4 位 1003 是一个自增的序列,从 1000 涨到 9000,
 * 到达 9000 后回到 1000 再开始往上涨.最后的 5 位 56696 是当前的进程 ID,为了防止单机多进程出现 TraceId 冲突的情况，所以在 TraceId 末尾添加了当前的进程 ID.
 */
public final class TraceIdGenerator {

    private static String IP_16 = "ffffffff";
    private static AtomicInteger count = new AtomicInteger(1000);

    private TraceIdGenerator() {

    }

    static {
        try {
            String ipAddress = TracerUtils.getInetAddress();
            if (ipAddress != null) {
                IP_16 = getIP_16(ipAddress);
            }
        } catch (Throwable e) {
        }
    }

    private static String getTraceId(String ip, long timestamp, int nextId) {
        StringBuilder appender = new StringBuilder(30);
        appender.append(ip).append(timestamp).append(nextId).append(TracerUtils.getPID());
        return appender.toString();
    }

    public static String generate() {
        return getTraceId(IP_16, System.nanoTime(), getNextId());
    }

    private static String getIP_16(String ip) {
        String[] ips = ip.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String column : ips) {
            String hex = Integer.toHexString(Integer.parseInt(column));
            if (hex.length() == 1) {
                sb.append('0').append(hex);
            } else {
                sb.append(hex);
            }
        }
        return sb.toString();
    }

    private static int getNextId() {
        for (; ; ) {
            int current = count.get();
            int next = (current > 9000) ? 1000 : current + 1;
            if (count.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
