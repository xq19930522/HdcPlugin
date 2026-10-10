# -*- coding: utf-8 -*-
"""构建 HDC Assistant 插件（与 ComponentGenerator 同一套路）。"""
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
DEVECO = os.environ.get("DEVECO_HOME", r"E:\DevEco Studio")
JAVAC = os.path.join(DEVECO, "jbr", "bin", "javac.exe")
OUT = os.path.join(ROOT, "out")
CLASSES = os.path.join(OUT, "classes")
JAR_PATH = os.path.join(OUT, "HdcAssistant.jar")
DIST = os.path.join(ROOT, "dist")
ZIP_PATH = os.path.join(DIST, "HdcAssistant-1.1.0.zip")


def find_sources():
    src = os.path.join(ROOT, "src")
    result = []
    for base, _, files in os.walk(src):
        for f in files:
            if f.endswith(".java"):
                result.append(os.path.join(base, f))
    return sorted(result)


def main():
    classpath = os.pathsep.join(
        os.path.join(DEVECO, "lib", name) for name in [
            "app.jar", "util.jar", "util_rt.jar", "util-8.jar", "annotations.jar",
        ]
    )
    for d in (CLASSES, DIST):
        shutil.rmtree(d, ignore_errors=True)
        os.makedirs(d, exist_ok=True)

    sources = find_sources()
    print(f"[1/3] 编译 {len(sources)} 个 Java 文件 ...")
    proc = subprocess.run([JAVAC, "-encoding", "UTF-8", "-nowarn",
                           "-cp", classpath, "-d", CLASSES] + sources,
                          capture_output=True, text=True)
    if proc.returncode != 0:
        print(proc.stdout)
        print(proc.stderr)
        sys.exit("编译失败")
    print("      编译成功")

    print("[2/3] 打包插件 jar ...")
    if os.path.exists(JAR_PATH):
        os.remove(JAR_PATH)
    with zipfile.ZipFile(JAR_PATH, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n\n")
        for base, _, files in os.walk(CLASSES):
            for f in files:
                full = os.path.join(base, f)
                zf.write(full, os.path.relpath(full, CLASSES).replace(os.sep, "/"))
        res = os.path.join(ROOT, "resources")
        for base, _, files in os.walk(res):
            for f in files:
                full = os.path.join(base, f)
                zf.write(full, os.path.relpath(full, res).replace(os.sep, "/"))
    print(f"      {JAR_PATH}")

    print("[3/3] 制作分发 zip ...")
    if os.path.exists(ZIP_PATH):
        os.remove(ZIP_PATH)
    with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.write(JAR_PATH, "HdcAssistant/lib/HdcAssistant.jar")
    print(f"      {ZIP_PATH}")
    print("完成。")


if __name__ == "__main__":
    main()
