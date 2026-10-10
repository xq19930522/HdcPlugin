package com.harmony.hdc;

/**
 * HDC 支持的设备操作，与 ADB Idea 的菜单一一对应。
 */
public enum Operation {
    UNINSTALL("Uninstall App"),
    INSTALL("Install App"),
    KILL("Kill App"),
    START("Start App"),
    RESTART("Restart App"),
    CLEAR_DATA("Clear App Data"),
    CLEAR_DATA_RESTART("Clear App Data and Restart"),
    START_DEBUG("Start App with Debugger"),
    RESTART_DEBUG("Restart App with Debugger"),
    CLEAR_DATA_RESTART_DEBUG("Clear App Data and Restart App with Debugger"),
    WIFI_ON("Enable Wi-Fi"),
    WIFI_OFF("Disable Wi-Fi"),
    DATA_ON("Enable Mobile Data"),
    DATA_OFF("Disable Mobile Data"),
    GET_UDID("Get Device UDID");

    private final String text;

    Operation(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /** 该操作是否需要 bundleName/abilityName（安装只需安装包，取 UDID 无需工程信息） */
    public boolean needsBundle() {
        return this != GET_UDID && this != INSTALL;
    }

    /** 该操作是否需要选择 .hap 安装包 */
    public boolean needsHap() {
        return this == INSTALL;
    }
}
