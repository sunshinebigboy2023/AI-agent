package com.yupi.yuaiagent.tools;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
public class ToolExecutionSupport {

    private static final Set<String> SENSITIVE_PATH_PREFIXES = Set.of(
            "/etc", "/proc", "/sys", "/var/run", "/root/.ssh", "/home", "/Users"
    );

    private final Path workspaceRoot;
    private final int maxObservationLength;

    public ToolExecutionSupport(Path workspaceRoot, int maxObservationLength) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.maxObservationLength = Math.max(200, maxObservationLength);
    }

    public String execute(String toolName, String argumentSummary, CheckedSupplier<String> supplier) {
        long start = System.currentTimeMillis();
        String safeArgs = summarize(argumentSummary);
        try {
            String result = truncate(supplier.get());
            log.info("Tool {} succeeded in {} ms, args={}", toolName, System.currentTimeMillis() - start, safeArgs);
            return result;
        } catch (Exception e) {
            log.warn("Tool {} failed in {} ms, args={}, error={}",
                    toolName, System.currentTimeMillis() - start, safeArgs, e.getMessage());
            return truncate("工具执行失败：" + e.getMessage());
        }
    }

    public Path resolveWorkspacePath(String fileName, String subDirectory) {
        if (StrUtil.isBlank(fileName)) {
            throw new IllegalArgumentException("fileName is required");
        }
        Path baseDir = workspaceRoot.resolve(subDirectory).normalize();
        Path resolvedPath = baseDir.resolve(fileName).normalize();
        ensureSafePath(resolvedPath);
        return resolvedPath;
    }

    public URI validateExternalUrl(String rawUrl) {
        URI uri = URI.create(rawUrl);
        String scheme = StrUtil.blankToDefault(uri.getScheme(), "").toLowerCase();
        if (!Set.of("http", "https").contains(scheme)) {
            throw new IllegalArgumentException("仅允许 http 或 https 地址");
        }
        String host = StrUtil.blankToDefault(uri.getHost(), "").toLowerCase();
        if (host.isBlank() || isPrivateOrSensitiveHost(host)) {
            throw new IllegalArgumentException("禁止访问本地或内网地址");
        }
        return uri;
    }

    public void validateTerminalCommand(String command) {
        String normalized = StrUtil.blankToDefault(command, "").toLowerCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("command is required");
        }
        if (normalized.contains("rm -rf")
                || normalized.contains("shutdown")
                || normalized.contains("reboot")
                || normalized.contains("printenv")
                || normalized.matches(".*(^|\\s)env(\\s|$).*")
                || normalized.contains("~/.ssh")
                || normalized.contains("/etc")
                || normalized.contains("127.0.0.1")
                || normalized.contains("localhost")
                || normalized.contains(">")
                || normalized.contains(">>")) {
            throw new IllegalArgumentException("命令包含高风险操作，已被拒绝");
        }
    }

    public String truncate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxObservationLength) {
            return text;
        }
        return text.substring(0, maxObservationLength) + "\n[内容过长，已截断]";
    }

    public String summarize(String text) {
        if (text == null) {
            return "";
        }
        String sanitized = text
                .replaceAll("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]\\s*\\S+", "$1=***")
                .replaceAll("\\s+", " ")
                .trim();
        return truncate(sanitized);
    }

    private void ensureSafePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("禁止访问工作目录之外的文件");
        }
        String normalizedPath = normalized.toString();
        if (SENSITIVE_PATH_PREFIXES.stream().anyMatch(normalizedPath::startsWith)) {
            throw new IllegalArgumentException("禁止访问敏感目录");
        }
    }

    private boolean isPrivateOrSensitiveHost(String host) {
        return host.equals("localhost")
                || host.endsWith(".local")
                || host.equals("0.0.0.0")
                || host.equals("127.0.0.1")
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*")
                || host.startsWith("169.254.")
                || host.equals("::1");
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
