package com.harmony.hdc;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 在 HarmonyOS 工程的构建产物里找最新的 .hap 安装包。
 * 典型路径：{@code <module>/build/default/outputs/default/<module>-default-signed.hap}。
 * 纯文件系统逻辑，不依赖 IntelliJ 平台，方便单测。
 */
public final class HapLocator {

    private static final String[] SKIP_DIRS = {
            "oh_modules", "node_modules", ".git", ".idea", ".hvigor", "hvigor", "libs"
    };

    private HapLocator() {
    }

    /**
     * 返回工程里最新修改的 .hap 文件绝对路径；找不到返回 null。
     */
    public static String findHap(String projectBasePath) {
        if (projectBasePath == null || projectBasePath.isBlank()) {
            return null;
        }
        Path root = Paths.get(projectBasePath);
        if (!Files.isDirectory(root)) {
            return null;
        }

        List<Path> haps = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                private int depth = 0;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }
                    depth++;
                    if (depth > 10) {
                        depth--;
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String name = dir.getFileName().toString();
                    for (String skip : SKIP_DIRS) {
                        if (skip.equals(name)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    depth--;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().toLowerCase().endsWith(".hap")) {
                        haps.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            return null;
        }

        Path newest = null;
        long newestTime = -1;
        for (Path hap : haps) {
            try {
                long t = Files.getLastModifiedTime(hap).toMillis();
                if (t > newestTime) {
                    newestTime = t;
                    newest = hap;
                }
            } catch (IOException ignored) {
                // 读不到修改时间的文件跳过
            }
        }
        return newest == null ? null : newest.toAbsolutePath().toString();
    }
}
