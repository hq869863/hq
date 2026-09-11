package org.example.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.InetAddress;
import java.util.*;

/**
 * @author hq
 */
@Component
public class SystemInfoTools {

    // 注入 Spring 的 Environment
    private final Environment environment;

    public SystemInfoTools(Environment environment) {
        this.environment = environment;
    }

    @Tool(description = "获取当前 JVM 的内存使用状态，包括堆内存和非堆内存的已用/最大值（单位：MB）")
    public Map<String, Object> getJvmMemoryStatus() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
        long heapMax = memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024;
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024;

        Map<String, Object> result = new HashMap<>();
        result.put("heapUsedMB", heapUsed);
        result.put("heapMaxMB", heapMax);
        result.put("heapUsagePercent", String.format("%.2f%%", (double) heapUsed / heapMax * 100));
        result.put("nonHeapUsedMB", nonHeapUsed);
        return result;
    }

    @Tool(description = "读取指定名称的配置项。支持读取 application.yml 中的所有配置。注意：读取嵌套配置时请使用点号分隔，例如：server.port, spring.datasource.url")
    public String getConfigValue(String configKey) {
        // 直接从 Spring 环境中获取配置，支持 yml 的层级结构（如 spring.datasource.url）
        String value = environment.getProperty(configKey);

        if (value != null) {
            return value;
        }
        return "配置项 [" + configKey + "] 不存在或未找到";
    }

    /**
     * 测试与指定 IP 或域名的网络连通性
     */
    @Tool(description = "测试与指定 IP 或域名的网络连通性。输入为 IP 地址或域名，返回连通状态和响应时间。")
    public Map<String, Object> testNetworkConnectivity(String host) {
        Map<String, Object> result = new HashMap<>();
        result.put("host", host);

        try {
            long startTime = System.currentTimeMillis();
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(3000);
            long endTime = System.currentTimeMillis();

            result.put("isReachable", reachable);
            result.put("responseTimeMs", endTime - startTime);
            result.put("ipAddress", address.getHostAddress());
            result.put("message", reachable ? "网络连通正常" : "无法连接到目标主机");
        } catch (Exception e) {
            result.put("isReachable", false);
            result.put("message", "检测出错：" + e.getMessage());
        }
        return result;
    }

    /**
     * 检查当前服务器各磁盘分区的空间使用情况
     */
    @Tool(description = "检查当前服务器各磁盘分区的空间使用情况，返回总空间、可用空间和使用率。")
    public List<Map<String, Object>> checkDiskSpace() {
        List<Map<String, Object>> diskInfoList = new ArrayList<>();
        File[] roots = File.listRoots();

        for (File root : roots) {
            Map<String, Object> diskInfo = new HashMap<>();
            diskInfo.put("partition", root.getAbsolutePath());

            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            diskInfo.put("totalGB", String.format("%.2f", totalSpace / 1024.0 / 1024.0 / 1024.0));
            diskInfo.put("freeGB", String.format("%.2f", freeSpace / 1024.0 / 1024.0 / 1024.0));
            diskInfo.put("usedGB", String.format("%.2f", usedSpace / 1024.0 / 1024.0 / 1024.0));
            diskInfo.put("usagePercent", String.format("%.2f%%", (double) usedSpace / totalSpace * 100));

            diskInfoList.add(diskInfo);
        }
        return diskInfoList;
    }

}