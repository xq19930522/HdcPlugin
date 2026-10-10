package com.harmony.hdc;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;

/**
 * 项目级设置，全部存在 PropertiesComponent 里：
 * hdc 路径、bundleName、abilityName 的手动覆盖值。
 * 任何一项为空就回到自动识别。
 */
public final class HdcSettings {

    private static final String KEY_HDC_PATH = "hdc.idea.hdcPath";
    private static final String KEY_BUNDLE = "hdc.idea.bundleName";
    private static final String KEY_ABILITY = "hdc.idea.abilityName";

    private HdcSettings() {
    }

    public static String getHdcPath(Project project) {
        return get(project, KEY_HDC_PATH);
    }

    public static String getBundleName(Project project) {
        return get(project, KEY_BUNDLE);
    }

    public static String getAbilityName(Project project) {
        return get(project, KEY_ABILITY);
    }

    public static void save(Project project, String hdcPath, String bundle, String ability) {
        PropertiesComponent pc = PropertiesComponent.getInstance(project);
        put(pc, KEY_HDC_PATH, hdcPath);
        put(pc, KEY_BUNDLE, bundle);
        put(pc, KEY_ABILITY, ability);
    }

    private static String get(Project project, String key) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        return PropertiesComponent.getInstance(project).getValue(key);
    }

    private static void put(PropertiesComponent pc, String key, String value) {
        if (value == null || value.isBlank()) {
            pc.unsetValue(key);
        } else {
            pc.setValue(key, value.trim());
        }
    }
}
