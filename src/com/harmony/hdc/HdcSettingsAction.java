package com.harmony.hdc;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Tools → HDC Idea → HDC Settings：手动覆盖 hdc 路径、bundleName、abilityName。
 * 留空表示自动识别。
 */
public class HdcSettingsAction extends AnAction implements DumbAware {

    public HdcSettingsAction() {
        super("HDC Settings...");
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
            new HdcSettingsDialog(project).show();
        }
    }
}

class HdcSettingsDialog extends DialogWrapper {

    private final Project project;
    private final JBTextField hdcPath = new JBTextField();
    private final JBTextField bundleName = new JBTextField();
    private final JBTextField abilityName = new JBTextField();

    HdcSettingsDialog(Project project) {
        super(project);
        this.project = project;
        setTitle("HDC Settings");
        String path = HdcPathLocator.findHdc(project);
        hdcPath.setText(path == null ? "" : path);
        bundleName.setText(nz(HdcSettings.getBundleName(project)));
        abilityName.setText(nz(HdcSettings.getAbilityName(project)));
        init();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("HDC path (auto-detect if empty):"), c);
        c.gridx = 1;
        panel.add(hdcPath, c);

        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Bundle name (auto-detect if empty):"), c);
        c.gridx = 1;
        panel.add(bundleName, c);

        c.gridx = 0; c.gridy = 2;
        panel.add(new JLabel("Ability name (for Start App):"), c);
        c.gridx = 1;
        panel.add(abilityName, c);
        return panel;
    }

    @Override
    protected void doOKAction() {
        HdcSettings.save(project, hdcPath.getText(), bundleName.getText(), abilityName.getText());
        HdcConsole.append(project, "HDC settings saved.");
        super.doOKAction();
    }
}
