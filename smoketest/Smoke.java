import com.harmony.hdc.HdcCommands;
import com.harmony.hdc.HapLocator;
import com.harmony.hdc.HdcPathLocator;
import com.harmony.hdc.Operation;
import com.harmony.hdc.ProjectModel;

import java.util.List;

public class Smoke {
    public static void main(String[] args) throws Exception {
        int failures = 0;

        // 1. 全部操作的命令序列
        System.out.println("== HdcCommands ==");
        for (Operation op : Operation.values()) {
            List<String[]> cmds = HdcCommands.build(op, "hdc", "com.demo.app", "EntryAbility", "demo.hap");
            System.out.println(op.getText());
            for (String[] c : cmds) {
                System.out.println("   " + HdcCommands.render(c));
            }
        }

        // 1a. 新增操作的命令断言
        List<String[]> install = HdcCommands.build(Operation.INSTALL, "hdc", "", "", "demo.hap");
        if (install.size() != 1 || !"hdc install -r demo.hap".equals(HdcCommands.render(install.get(0)))) {
            System.out.println("FAIL: install cmd -> " + HdcCommands.render(install.get(0)));
            failures++;
        }
        List<String[]> udid = HdcCommands.build(Operation.GET_UDID, "hdc", "", "");
        if (udid.size() != 1 || !"hdc shell bm get --udid".equals(HdcCommands.render(udid.get(0)))) {
            System.out.println("FAIL: udid cmd -> " + HdcCommands.render(udid.get(0)));
            failures++;
        }
        try {
            HdcCommands.build(Operation.INSTALL, "hdc", "", "", "");
            System.out.println("FAIL: install without hap should throw");
            failures++;
        } catch (IllegalArgumentException expected) {
            // ok
        }

        // 1b. 安装包自动识别：最新修改的 .hap
        System.out.println("== HapLocator ==");
        java.nio.file.Path hapRoot = java.nio.file.Files.createTempDirectory("haptest");
        java.nio.file.Path outDir = hapRoot.resolve("entry/build/default/outputs/default");
        java.nio.file.Files.createDirectories(outDir);
        java.nio.file.Path old = outDir.resolve("entry-old.hap");
        java.nio.file.Path newest = outDir.resolve("entry-default-signed.hap");
        java.nio.file.Files.writeString(old, "old");
        java.nio.file.Files.writeString(newest, "new");
        java.nio.file.Files.setLastModifiedTime(old,
                java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 100_000));
        String foundHap = HapLocator.findHap(hapRoot.toString());
        System.out.println("hap = " + foundHap);
        if (foundHap == null || !foundHap.endsWith("entry-default-signed.hap")) {
            System.out.println("FAIL: hap locator");
            failures++;
        }
        if (HapLocator.findHap("F:/no_such_dir") != null) {
            System.out.println("FAIL: hap locator empty dir");
            failures++;
        }

        // 2. 移动数据网卡识别
        String ifconfig = "lo        Link encap:Local Loopback\n" +
                "wlan0     Link encap:Ethernet  HWaddr 02:00:00:00:00:00\n" +
                "rmnet_data0 Link encap:Ethernet  HWaddr 52:02:7b:9e:0a:0b\n" +
                "p2p0      Link encap:Ethernet\n";
        String iface = HdcCommands.pickMobileIface(ifconfig);
        System.out.println("pickMobileIface -> " + iface);
        if (!"rmnet_data0".equals(iface)) { System.out.println("FAIL: iface"); failures++; }
        if (HdcCommands.pickMobileIface("wlan0 Link encap:Ethernet") != null) { System.out.println("FAIL: no-iface case"); failures++; }

        // 3. 真实工程识别
        System.out.println("== ProjectModel.detect ==");
        ProjectModel.Model m = ProjectModel.detect("F:/HarmonyProject/harmony_project");
        System.out.println("bundle = " + m.bundleName() + ", ability = " + m.abilityName());
        if (!"com.hjq.demo.harmony".equals(m.bundleName())) { System.out.println("FAIL: bundle"); failures++; }
        if (!"EntryAbility".equals(m.abilityName())) { System.out.println("FAIL: ability"); failures++; }

        ProjectModel.Model empty = ProjectModel.detect("F:/no_such_dir");
        if (empty.hasBundle()) { System.out.println("FAIL: empty dir should be empty"); failures++; }

        // 4. hdc 路径搜索（含 IDE 自带 SDK）
        System.out.println("== HdcPathLocator ==");
        String localAppData = System.getenv("LOCALAPPDATA");
        String found = HdcPathLocator.searchInRoots(
                List.of("E:/DevEco Studio/sdk", localAppData + "/Huawei/Sdk"));
        System.out.println("hdc = " + found);
        if (found == null || !found.endsWith("hdc.exe")) { System.out.println("FAIL: hdc not found"); failures++; }

        // 5. 不带 mainElement 的 module.json5 兜底 abilities[0]
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("smoke");
        java.nio.file.Path src = tmp.resolve("entry/src/main");
        java.nio.file.Files.createDirectories(src);
        java.nio.file.Files.writeString(src.resolve("module.json5"),
                "{ \"module\": { \"abilities\": [ { \"name\": \"MainAbility2\" } ] } }");
        java.nio.file.Files.createDirectories(tmp.resolve("AppScope"));
        java.nio.file.Files.writeString(tmp.resolve("AppScope/app.json5"),
                "{ \"app\": { \"bundleName\": \"com.fallback.test\" } }");
        ProjectModel.Model fb = ProjectModel.detect(tmp.toString());
        System.out.println("fallback: bundle=" + fb.bundleName() + " ability=" + fb.abilityName());
        if (!"com.fallback.test".equals(fb.bundleName()) || !"MainAbility2".equals(fb.abilityName())) {
            System.out.println("FAIL: fallback detection"); failures++;
        }

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        if (failures > 0) System.exit(1);
    }
}
