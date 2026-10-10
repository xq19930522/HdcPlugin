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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 HarmonyOS 工程里识别 bundleName 和启动 Ability 名称。
 * 识别规则（覆盖 API 9+ 的 module.json5 工程和更老的 config.json 工程）：
 * <ul>
 *   <li>bundleName：AppScope/app.json5 的 app.bundleName，兜底找任意 config.json</li>
 *   <li>ability：模块 module.json5 的 module.mainElement，兜底 abilities[0].name</li>
 * </ul>
 * JSON5 里有注释，所以用正则抽字段，不整文件解析。
 */
public final class ProjectModel {

    private static final Pattern BUNDLE = Pattern.compile("\"bundleName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MAIN_ELEMENT = Pattern.compile("\"mainElement\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ABILITIES_BLOCK = Pattern.compile("\"abilities\"\\s*:\\s*\\[.*?\"name\"\\s*:\\s*\"([^\"]+)\"");

    private static final String[] SKIP_DIRS = {
            "oh_modules", "node_modules", "build", ".git", ".idea", "hvigor", ".hvigor", "libs"
    };

    public record Model(String bundleName, String abilityName) {
        public boolean hasBundle() {
            return bundleName != null && !bundleName.isBlank();
        }
    }

    private ProjectModel() {
    }

    public static Model detect(String projectBasePath) {
        if (projectBasePath == null || projectBasePath.isBlank()) {
            return new Model(null, null);
        }
        Path root = Paths.get(projectBasePath);
        if (!Files.isDirectory(root)) {
            return new Model(null, null);
        }

        List<Path> appJson5 = new ArrayList<>();
        List<Path> moduleJson5 = new ArrayList<>();
        List<Path> configJson = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                private int depth = 0;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }
                    depth++;
                    if (depth > 8) {
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
                    String name = file.getFileName().toString();
                    if ("app.json5".equals(name) && file.getParent().getFileName().toString().equals("AppScope")) {
                        appJson5.add(file);
                    } else if ("module.json5".equals(name)) {
                        moduleJson5.add(file);
                    } else if ("config.json".equals(name)) {
                        configJson.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // 目录读不到就返回空模型，由上层弹设置框
        }

        String bundle = firstMatch(appJson5, BUNDLE);
        if (bundle == null) {
            bundle = firstMatch(configJson, BUNDLE);
        }
        String ability = null;
        for (Path p : moduleJson5) {
            String content = read(p);
            if (content == null) {
                continue;
            }
            ability = match(content, MAIN_ELEMENT);
            if (ability == null) {
                ability = match(content, ABILITIES_BLOCK);
            }
            if (ability != null) {
                break;
            }
        }
        return new Model(bundle, ability);
    }

    private static String firstMatch(List<Path> files, Pattern p) {
        for (Path f : files) {
            String m = match(read(f), p);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    private static String match(String content, Pattern p) {
        if (content == null) {
            return null;
        }
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            return null;
        }
    }
}
