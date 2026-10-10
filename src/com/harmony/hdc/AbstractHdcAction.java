package com.harmony.hdc;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 所有设备操作的基类：解析 hdc 路径和工程信息，然后交给 HdcRunner 执行。
 */
public abstract class AbstractHdcAction extends AnAction implements DumbAware {

    private final Operation operation;

    protected AbstractHdcAction(Operation operation) {
        super(operation.getText());
        this.operation = operation;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            execute(project, operation);
        }
    }

    /** 菜单 Action 和 Operations Popup 共用的执行入口 */
    public static void execute(Project project, Operation operation) {

        String hdc = HdcPathLocator.findHdc(project);
        if (hdc == null) {
            HdcConsole.append(project, "[error] hdc not found. Set it in Tools → HDC Assistant → HDC Settings");
            boolean ok = new HdcSettingsDialog(project).showAndGet();
            if (ok) {
                hdc = HdcPathLocator.findHdc(project);
            }
            if (hdc == null) {
                return;
            }
        }

        // 安装：先在构建产物里找最新 .hap，找不到让用户手选
        String hapPath = null;
        if (operation.needsHap()) {
            hapPath = resolveHap(project);
            if (hapPath == null) {
                // 用户取消了文件选择
                return;
            }
        }

        // 部分操作（Install / Get UDID）不需要 bundleName
        ProjectModel.Model model = null;
        if (operation.needsBundle()) {
            model = resolveModel(project);
            if (model == null) {
                // 用户取消了设置框
                return;
            }
        }
        String bundle = model == null ? "" : model.bundleName();
        String ability = model == null ? "" : model.abilityName();

        HdcConsole.append(project, "");
        HdcRunner.run(project, operation,
                HdcCommands.build(operation, hdc, bundle, ability, hapPath));
    }

    /** 安装包解析：自动识别优先，识别不到弹文件选择框 */
    private static String resolveHap(Project project) {
        String detected = HapLocator.findHap(project.getBasePath());
        if (detected != null) {
            HdcConsole.append(project, "[info] hap found: " + detected);
            return detected;
        }
        HdcConsole.append(project, "[warn] no .hap found in build outputs, please choose one");

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter(file -> file.getName().toLowerCase().endsWith(".hap"))
                .withTitle("Select HAP File to Install");
        VirtualFile chosen = FileChooser.chooseFile(descriptor, project, null);
        if (chosen == null) {
            HdcConsole.append(project, "[canceled] no hap selected");
            return null;
        }
        return chosen.getPath();
    }

    /** 设置覆盖优先，其次自动识别；识别不到就弹设置框 */
    private static ProjectModel.Model resolveModel(Project project) {
        String bundle = HdcSettings.getBundleName(project);
        String ability = HdcSettings.getAbilityName(project);
        if (bundle == null || bundle.isBlank()) {
            ProjectModel.Model detected = ProjectModel.detect(project.getBasePath());
            if (detected.hasBundle()) {
                bundle = detected.bundleName();
                if (ability == null || ability.isBlank()) {
                    ability = detected.abilityName();
                }
            }
        }
        if (bundle == null || bundle.isBlank()) {
            HdcConsole.append(project, "[error] bundleName not detected. Configure it in HDC Settings.");
            if (!new HdcSettingsDialog(project).showAndGet()) {
                return null;
            }
            bundle = HdcSettings.getBundleName(project);
            ability = HdcSettings.getAbilityName(project);
        }
        if (bundle == null || bundle.isBlank()) {
            HdcConsole.append(project, "[error] bundleName is still empty, operation aborted.");
            return null;
        }
        return new ProjectModel.Model(bundle, ability);
    }
}
