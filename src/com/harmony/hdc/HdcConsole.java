package com.harmony.hdc;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * HDC 工具窗口的控制台面板。每个项目一个实例，输出按行追加。
 */
public class HdcConsole extends JPanel {

    private static final Map<Project, HdcConsole> REGISTRY = new WeakHashMap<>();

    private final JBTextArea text = new JBTextArea();

    public HdcConsole() {
        super(new BorderLayout());
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        add(new JBScrollPane(text), BorderLayout.CENTER);
    }

    static void register(Project project, HdcConsole console) {
        synchronized (REGISTRY) {
            REGISTRY.put(project, console);
        }
    }

    static void append(Project project, String line) {
        HdcConsole console;
        synchronized (REGISTRY) {
            console = REGISTRY.get(project);
        }
        if (console == null) {
            return;
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            console.text.append(line);
            if (!line.endsWith("\n")) {
                console.text.append("\n");
            }
            console.text.setCaretPosition(console.text.getDocument().getLength());
        });
    }

    static void clear(Project project) {
        HdcConsole console;
        synchronized (REGISTRY) {
            console = REGISTRY.get(project);
        }
        if (console != null) {
            javax.swing.SwingUtilities.invokeLater(() -> console.text.setText(""));
        }
    }
}
