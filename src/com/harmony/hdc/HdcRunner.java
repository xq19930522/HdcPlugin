package com.harmony.hdc;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 后台执行 HDC 命令序列，输出进 HDC 控制台，结束时弹通知。
 */
public final class HdcRunner {

    private static final long TIMEOUT_MIN = 2;

    private HdcRunner() {
    }

    public static void run(Project project, Operation op, List<String[]> commands) {
        String title = op.getText();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String lastOutput = null;
                for (String[] raw : commands) {
                    if (indicator.isCanceled()) {
                        HdcConsole.append(project, "[canceled]");
                        return;
                    }
                    String[] argv = raw;
                    if (HdcCommands.needsIface(op)) {
                        argv = resolveIface(project, raw);
                        if (argv == null) {
                            return;
                        }
                    }
                    lastOutput = execute(project, argv, indicator);
                }
                if (op == Operation.GET_UDID) {
                    copyUdidToClipboard(project, lastOutput);
                }
                notifyUser(project, title, false, null);
            }

            @Override
            public void onCancel() {
                notifyUser(project, title, true, null);
            }
        });
    }

    /** 移动数据操作：先 ifconfig 找到网卡名，替换占位符 */
    private static String[] resolveIface(Project project, String[] raw) {
        String hdc = raw[0];
        HdcConsole.append(project, "$ " + hdc + " shell ifconfig  (detecting mobile interface)");
        String output = runProcess(new String[]{hdc, "shell", "ifconfig"}, null);
        if (output == null) {
            HdcConsole.append(project, "[error] failed to run ifconfig");
            notifyUser(project, "Mobile data", true, "ifconfig 执行失败");
            return null;
        }
        String iface = HdcCommands.pickMobileIface(output);
        if (iface == null) {
            HdcConsole.append(project, "[error] no mobile data interface found in ifconfig output");
            notifyUser(project, "Mobile data", true, "未在 ifconfig 输出里找到移动数据网卡");
            return null;
        }
        HdcConsole.append(project, "  mobile interface: " + iface);
        String[] fixed = new String[raw.length];
        System.arraycopy(raw, 0, fixed, 0, raw.length);
        fixed[2] = fixed[2].replace(HdcCommands.IFACE_PLACEHOLDER, iface);
        return fixed;
    }

    private static String execute(Project project, String[] argv, ProgressIndicator indicator) {
        HdcConsole.append(project, "$ " + HdcCommands.render(argv));
        String output = runProcess(argv, indicator);
        if (output == null) {
            HdcConsole.append(project, "[error] hdc process failed to start (hdc not found?)");
            notifyUser(project, HdcCommands.render(argv), true,
                    "hdc 启动失败，请到 Tools → HDC Idea → HDC Settings 检查路径");
            return null;
        }
        if (!output.isBlank()) {
            for (String line : output.split("\n")) {
                HdcConsole.append(project, "  " + line);
            }
        }
        return output;
    }

    /** 取最后一行非空输出作为 UDID 复制进剪贴板，方便直接粘贴使用 */
    private static void copyUdidToClipboard(Project project, String output) {
        if (output == null) {
            return;
        }
        String udid = null;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                udid = trimmed;
            }
        }
        if (udid == null) {
            HdcConsole.append(project, "[error] no udid returned by device");
            notifyUser(project, "Get Device UDID", true, "设备未返回 UDID");
            return;
        }
        String finalUdid = udid;
        ApplicationManager.getApplication().invokeLater(() -> {
            com.intellij.openapi.ide.CopyPasteManager.getInstance()
                    .setContents(new java.awt.datatransfer.StringSelection(finalUdid));
            HdcConsole.append(project, "[info] UDID copied to clipboard: " + finalUdid);
        });
    }

    /** 运行进程并收集输出；返回 null 表示启动失败 */
    private static String runProcess(String[] argv, ProgressIndicator indicator) {
        try {
            ProcessBuilder pb = new ProcessBuilder(argv).redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (indicator != null && indicator.isCanceled()) {
                        process.destroy();
                        break;
                    }
                }
            }
            if (!process.waitFor(TIMEOUT_MIN, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                sb.append("[error] timed out after ").append(TIMEOUT_MIN).append(" min");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void notifyUser(Project project, String title, boolean error, String detail) {
        ApplicationManager.getApplication().invokeLater(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("HDC Idea")
                        .createNotification(title, detail == null ? "" : detail,
                                error ? NotificationType.ERROR : NotificationType.INFORMATION)
                        .notify(project));
    }
}
