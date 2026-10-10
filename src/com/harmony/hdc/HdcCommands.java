package com.harmony.hdc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 纯逻辑的 HDC 命令构建器，不依赖 IntelliJ 平台，方便单测。
 * 每个操作返回按顺序执行的 argv 列表；移动数据操作里的 {IFACE}
 * 由执行器在运行前通过 hdc shell ifconfig 解析替换。
 */
public final class HdcCommands {

    /** 移动数据命令里的网卡占位符 */
    public static final String IFACE_PLACEHOLDER = "{IFACE}";

    private HdcCommands() {
    }

    public static List<String[]> build(Operation op, String hdc, String bundle, String ability) {
        return build(op, hdc, bundle, ability, null);
    }

    public static List<String[]> build(Operation op, String hdc, String bundle, String ability, String hapPath) {
        List<String[]> cmds = new ArrayList<>();
        switch (op) {
            case UNINSTALL -> cmds.add(new String[]{hdc, "uninstall", bundle});
            case INSTALL -> {
                if (hapPath == null || hapPath.isBlank()) {
                    throw new IllegalArgumentException("install requires a hap path");
                }
                cmds.add(new String[]{hdc, "install", "-r", hapPath});
            }
            case KILL -> cmds.add(shell(hdc, "aa force-stop " + bundle));
            case START -> cmds.add(startCmd(hdc, bundle, ability, false));
            case RESTART -> {
                cmds.add(shell(hdc, "aa force-stop " + bundle));
                cmds.add(startCmd(hdc, bundle, ability, false));
            }
            case CLEAR_DATA -> cmds.add(clearDataCmd(hdc, bundle));
            case CLEAR_DATA_RESTART -> {
                cmds.add(clearDataCmd(hdc, bundle));
                cmds.add(startCmd(hdc, bundle, ability, false));
            }
            case START_DEBUG -> cmds.add(startCmd(hdc, bundle, ability, true));
            case RESTART_DEBUG -> {
                cmds.add(shell(hdc, "aa force-stop " + bundle));
                cmds.add(startCmd(hdc, bundle, ability, true));
            }
            case CLEAR_DATA_RESTART_DEBUG -> {
                cmds.add(clearDataCmd(hdc, bundle));
                cmds.add(shell(hdc, "aa force-stop " + bundle));
                cmds.add(startCmd(hdc, bundle, ability, true));
            }
            case WIFI_ON -> cmds.add(shell(hdc, "ifconfig wlan0 up"));
            case WIFI_OFF -> cmds.add(shell(hdc, "ifconfig wlan0 down"));
            case DATA_ON -> cmds.add(shell(hdc, "ifconfig " + IFACE_PLACEHOLDER + " up"));
            case DATA_OFF -> cmds.add(shell(hdc, "ifconfig " + IFACE_PLACEHOLDER + " down"));
            case GET_UDID -> cmds.add(shell(hdc, "bm get --udid"));
            default -> throw new IllegalArgumentException("unknown operation: " + op);
        }
        return cmds;
    }

    /** 当前操作是否会用到移动数据网卡占位符 */
    public static boolean needsIface(Operation op) {
        return op == Operation.DATA_ON || op == Operation.DATA_OFF;
    }

    /**
     * 从 ifconfig 输出里挑移动数据网卡。真机常见 rmnet/pdp，
     * 部分平台是 ccmni/ncm/cellular/wwan。
     */
    public static String pickMobileIface(String ifconfigOutput) {
        for (String line : ifconfigOutput.split("\n")) {
            String name = line.trim();
            int colon = name.indexOf(' ');
            if (colon > 0) {
                name = name.substring(0, colon);
            }
            name = name.replace(":", "").trim();
            if (name.matches("(?i).*(rmnet|ccmni|ncm|cellular|wwan|pdp).*")) {
                return name;
            }
        }
        return null;
    }

    public static String render(String[] argv) {
        return String.join(" ", Arrays.asList(argv));
    }

    private static String[] shell(String hdc, String shellCommand) {
        return new String[]{hdc, "shell", shellCommand};
    }

    private static String[] startCmd(String hdc, String bundle, String ability, boolean debug) {
        String cmd = "aa start -b " + bundle + " -a " + ability + (debug ? " -D" : "");
        return shell(hdc, cmd);
    }

    private static String[] clearDataCmd(String hdc, String bundle) {
        return shell(hdc, "bm clean -d " + bundle);
    }
}
