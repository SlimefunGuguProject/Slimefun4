#!/usr/bin/env python3
"""Build a cloned Gradle or Maven addon against the exact local Slimefun JAR."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path
import xml.etree.ElementTree as ET


def run(*cmd: str, cwd: Path, env: dict[str, str] | None = None) -> None:
    subprocess.run(cmd, cwd=cwd, env=env, check=True)


def patch_maven_dependencies(project: Path) -> bool:
    pom = project / "pom.xml"
    tree = ET.parse(pom)
    root = tree.getroot()
    namespace = root.tag.partition("}")[0].strip("{") if "}" in root.tag else ""
    if namespace:
        ET.register_namespace("", namespace)
    changed = False
    for dep in root.iter():
        if dep.tag.split("}")[-1] != "dependency":
            continue
        children = {child.tag.split("}")[-1]: child for child in dep}
        group = (children.get("groupId").text or "") if children.get("groupId") is not None else ""
        artifact = (children.get("artifactId").text or "") if children.get("artifactId") is not None else ""
        identity = f"{group}:{artifact}".lower()
        if "slimefun" not in identity or "slimefuncomplib" in identity:
            continue
        children["groupId"].text = "com.github.slimefun"
        children["artifactId"].text = "Slimefun"
        version = children.get("version")
        if version is None:
            version = ET.SubElement(dep, f"{{{namespace}}}version" if namespace else "version")
        version.text = "Legacy-CI"
        scope = children.get("scope")
        if scope is not None and (scope.text or "").strip() == "system":
            scope.text = "provided"
        system_path = children.get("systemPath")
        if system_path is not None:
            dep.remove(system_path)
        changed = True
    if changed:
        tree.write(pom, encoding="utf-8", xml_declaration=True)
    return changed

def build_maven(project: Path, jar: Path) -> None:
    if not patch_maven_dependencies(project):
        raise RuntimeError("No Slimefun Maven dependency was found")
    run(
        "mvn", "-B", "install:install-file",
        f"-Dfile={jar}", "-DgroupId=com.github.slimefun", "-DartifactId=Slimefun",
        "-Dversion=Legacy-CI", "-Dpackaging=jar", "-DgeneratePom=true",
        cwd=project,
    )
    wrapper = project / "mvnw"
    if wrapper.exists():
        wrapper.chmod(wrapper.stat().st_mode | 0o111)
        run(str(wrapper), "-B", "-DskipTests", "package", cwd=project)
    else:
        run("mvn", "-B", "-DskipTests", "package", cwd=project)

def build_gradle(project: Path, jar: Path) -> None:
    init_script = project / ".slimefun-legacy-ci.init.gradle"
    init_script.write_text(
        """
allprojects {
    afterEvaluate { p ->
        p.configurations.each { configuration ->
            def matches = configuration.dependencies.findAll { dependency ->
                def id = ((dependency.group ?: '') + ':' + dependency.name).toLowerCase()
                id.contains('slimefun') && !id.contains('slimefuncomplib')
            }
            if (!matches.isEmpty()) {
                matches.each { configuration.dependencies.remove(it) }
                p.dependencies.add(configuration.name, p.files(System.getenv('SLIMEFUN_LEGACY_JAR')))
            }
        }
    }
}
""".strip() + "\n",
        encoding="utf-8",
    )
    env = dict(os.environ)
    env["SLIMEFUN_LEGACY_JAR"] = str(jar)
    wrapper = project / "gradlew"
    if wrapper.exists():
        wrapper.chmod(wrapper.stat().st_mode | 0o111)
        run(str(wrapper), "clean", "assemble", "--no-daemon", "-I", str(init_script), cwd=project, env=env)
    else:
        run("gradle", "clean", "assemble", "--no-daemon", "-I", str(init_script), cwd=project, env=env)


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: build_addon_against_local_slimefun.py <addon-dir> <slimefun.jar>", file=sys.stderr)
        return 2
    project = Path(sys.argv[1]).resolve()
    jar = Path(sys.argv[2]).resolve()
    if (project / "pom.xml").exists():
        build_maven(project, jar)
    elif (project / "build.gradle").exists() or (project / "build.gradle.kts").exists():
        build_gradle(project, jar)
    else:
        raise RuntimeError("Unsupported addon build system")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
