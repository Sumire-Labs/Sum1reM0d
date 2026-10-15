"""Build and publication matrices from the template's properties (stdlib only)."""
import argparse
import json
import os
from pathlib import Path


def properties(path):
    result = {}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def csv(value):
    return [part.strip() for part in value.split(",") if part.strip()]


def flag(settings, key):
    value = settings.get(key, "false")
    if value not in ("true", "false"):
        raise ValueError(f"{key} must be true or false")
    return value == "true"


def matrices(publishing=False):
    root = properties("gradle.properties")
    enabled = {site: flag(root, f"publish_{site}") for site in ("modrinth", "curseforge")}
    if publishing and not any(enabled.values()):
        raise ValueError("Enable publish_modrinth and/or publish_curseforge in gradle.properties")
    for site, active in enabled.items():
        if publishing and active and not os.environ.get(f"{site.upper()}_TOKEN"):
            raise ValueError(f"Missing Actions secret: {site.upper()}_TOKEN")
    if root.get("release_type", "release") not in ("release", "beta", "alpha"):
        raise ValueError("release_type must be release, beta, or alpha")
    build, merge, publish = [], [], []
    for mc in csv(properties("versions/support.properties")["minecraft_versions"]):
        pins = properties(f"versions/{mc}/gradle.properties")
        loaders = csv(pins["loaders"])
        build.extend({"target": f"{mc}-{loader}"} for loader in loaders)
        merging = flag(root, "enable_forgix")
        merged_loaders = [loader for loader in loaders if loader in csv(root["forgix_loaders"])]
        units = []
        if merging and len(merged_loaders) >= 2:
            merge.append({"minecraft": mc, "targets": ",".join(f"{mc}-{loader}" for loader in merged_loaders)})
            units.append(("merged", merged_loaders))
            units.extend((loader, [loader]) for loader in loaders if loader not in merged_loaders)
        else:
            units.extend((loader, [loader]) for loader in loaders)
        for label, unit_loaders in units:
            item = {
                "artifact": f"{mc}-{label}", "minecraft": mc,
                "loaders": "\n".join(unit_loaders), "java": pins["java_version"],
                "release_type": root.get("release_type", "release"),
            }
            for site, active in enabled.items():
                project_id = pins.get(f"{site}_project_id", root.get(f"{site}_project_id", ""))
                if publishing and active and not project_id:
                    raise ValueError(f"Missing {site}_project_id for {mc}")
                item[f"{site}_id"] = project_id if active else ""
            publish.append(item)
    return {
        "build": {"include": build}, "merge": {"include": merge},
        "has_merge": str(bool(merge)).lower(), "publish": {"include": publish},
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    outputs = matrices(args.publish)
    lines = [f"{key}={json.dumps(value, separators=(',', ':')) if isinstance(value, dict) else value}"
             for key, value in outputs.items()]
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write("\n".join(lines) + "\n")
    else:
        print("\n".join(lines))
