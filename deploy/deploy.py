#!/usr/bin/env python3
"""Deploy only the Coachly workouts backend from the Coachly checkout root."""

from __future__ import annotations

import json
import os
import pathlib
import subprocess
import sys
import time
import urllib.request


SERVICE = "coachly-workouts-be"
IMAGE = "coachly-coachly-workouts-be:latest"


def run(*args: str, cwd: pathlib.Path, env: dict[str, str] | None = None) -> None:
    subprocess.run(args, cwd=cwd, env=env, check=True)


def container_environment(root: pathlib.Path) -> dict[str, str]:
    output = subprocess.check_output(
        ["docker", "inspect", SERVICE, "--format", "{{json .Config.Env}}"],
        cwd=root,
        text=True,
    )
    values = json.loads(output)
    return dict(item.split("=", 1) for item in values if "=" in item)


def apply_migration(root: pathlib.Path) -> None:
    values = container_environment(root)
    jdbc_url = values["COACHLY_DB_URL"]
    postgres_url = jdbc_url.removeprefix("jdbc:")
    environment = os.environ.copy()
    environment["PGPASSWORD"] = values["COACHLY_DB_PASSWORD"]
    run(
        "psql",
        postgres_url,
        "--username",
        values["COACHLY_DB_USERNAME"],
        "--set",
        "ON_ERROR_STOP=1",
        "--file",
        str(
            root
            / "services/coachly-workouts-be/deploy/migrations/V2__workout_programming.sql"
        ),
        cwd=root,
        env=environment,
    )


def wait_for_health() -> None:
    deadline = time.monotonic() + 90
    url = "http://127.0.0.1:9101/actuator/health"
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=3) as response:
                payload = json.load(response)
                if response.status == 200 and payload.get("status") == "UP":
                    return
        except Exception:
            pass
        time.sleep(2)
    raise RuntimeError(f"{SERVICE} did not become healthy within 90 seconds")


def main() -> int:
    root = pathlib.Path.cwd().resolve()
    expected = root / "services/coachly-workouts-be"
    if not expected.is_dir():
        print("Run deploy.py from the Coachly checkout root.", file=sys.stderr)
        return 2

    apply_migration(root)
    run(
        "docker",
        "build",
        "--file",
        "services/coachly-workouts-be/Dockerfile.deploy",
        "--tag",
        IMAGE,
        ".",
        cwd=root,
    )
    run(
        "docker",
        "compose",
        "up",
        "--detach",
        "--no-deps",
        "--force-recreate",
        "--no-build",
        SERVICE,
        cwd=root,
    )
    wait_for_health()
    print(f"{SERVICE} deployed and healthy")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
