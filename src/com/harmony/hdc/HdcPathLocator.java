package com.harmony.hdc;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 定位 hdc 可执行文件。查找顺序：
 * <ol>
 *   <li>用户在 HDC Settings 里手动指定的路径</li>
 *   <li>当前 IDE 安装目录下的 sdk（DevEco Studio 自带 SDK：sdk/default/openharmony/toolchains）</li>
 *   <li>用户目录下的 HarmonyOS SDK（%LOCALAPPDATA%\Huawei\Sdk）</li>
 * </ol>
 */
public final class HdcPathLocator {

    private static final String[] HDC_RELATIVE = {
            "default/openharmony/toolchains/hdc.exe",
            "default/hms/toolchains/hdc.exe",
            "openharmony/toolchains/hdc.exe",
            "toolchains/hdc.exe",
            "tools/hdc.exe",
    };

    private HdcPathLocator() {
    }

    public static String findHdc(Project project) {
        String override = HdcSettings.getHdcPath(project);
        if (override != null && !override.isBlank() && new File(override).isFile()) {
            return override;
        }

        List<String> roots = new ArrayList<>();
        try {
            String home = PathManager.getHomePath();
            if (home != null) {
                roots.add(new File(home, "sdk").getPath());
            }
        } catch (Throwable ignored) {
            // 非 IDE 环境（单测）下没有 PathManager，继续走后缀路径
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            roots.add(new File(localAppData, "Huawei/Sdk").getPath());
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            roots.add(new File(userHome, "Huawei/Sdk").getPath());
        }
        return searchInRoots(roots);
    }

    /** 纯文件系统查找，便于脱离 IDE 测试 */
    public static String searchInRoots(List<String> sdkRoots) {
        for (String root : sdkRoots) {
            if (root == null) {
                continue;
            }
            File sdkDir = new File(root);
            // 直接子目录（default、12、13 这类 api 版本目录）
            File[] children = sdkDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    for (String rel : HDC_RELATIVE) {
                        File candidate = new File(child, rel);
                        if (candidate.isFile()) {
                            return candidate.getAbsolutePath();
                        }
                    }
                }
            }
            // sdk 根本身
            for (String rel : HDC_RELATIVE) {
                File candidate = new File(sdkDir, rel);
                if (candidate.isFile()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }
}
