package com.harmony.hdc;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * HDC Operations Popup：居中弹出可搜索的操作列表，对应 ADB Idea 的同名入口。
 * 方向键选择、回车执行、直接输入字母过滤。
 */
public class HdcOperationsPopupAction extends AnAction implements DumbAware {

    public HdcOperationsPopupAction() {
        super("HDC Operations Popup...");
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
        if (project == null) {
            return;
        }
        List<Operation> operations = Arrays.asList(Operation.values());
        JBList<Operation> list = new JBList<>(operations);
        list.setSelectedIndex(0);

        JBPopup popup = JBPopupFactory.getInstance()
                .createPopupChooserBuilder(operations)
                .setTitle("HDC Operations")
                .setNamerForFiltering(Operation::getText)
                .setItemChosenCallback(op -> runOperation(project, op))
                .createPopup();

        popup.showCenteredInCurrentWindow(project);
    }

    private static void runOperation(Project project, Operation op) {
        AbstractHdcAction.execute(project, op);
    }
}
