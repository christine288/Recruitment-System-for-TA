#!/usr/bin/env python3
"""Run local Tomcat with Doubao credentials from the Git-ignored root .env."""

import os
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ARK_KEYS = ("ARK_API_KEY", "ARK_MODEL")


def read_ark_env(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise SystemExit("Missing root .env; add ARK_API_KEY and ARK_MODEL first.")

    values = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        key = key.strip()
        if not separator or key not in ARK_KEYS:
            continue
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        values[key] = value

    missing = [key for key in ARK_KEYS if not values.get(key)]
    if missing:
        raise SystemExit("Missing nonempty " + ", ".join(missing) + " in root .env.")
    return values


def main() -> None:
    values = read_ark_env(ROOT / ".env")
    maven = shutil.which("mvn")
    if not maven:
        fallback = Path("/private/tmp/apache-maven-3.9.9/bin/mvn")
        if fallback.is_file() and os.access(fallback, os.X_OK):
            maven = str(fallback)
    if not maven:
        raise SystemExit("Maven not found; install Maven 3.9+ or add mvn to PATH.")

    env = os.environ.copy()
    env.pop("LM_API_KEY", None)
    env.pop("LM_MODEL", None)
    env.update(values)
    env["LM_PROVIDER"] = "doubao"
    os.chdir(ROOT)
    os.execve(maven, [maven, "-DskipTests", "tomcat7:run-war"], env)


if __name__ == "__main__":
    main()
