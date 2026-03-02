import os
import sys
import threading
import subprocess
from pathlib import Path

threads = []


def find_project_roots(root, max_depth=3):
    """Find candidate project directories that contain a build file or wrapper.

    We search up to `max_depth` levels from `root`.
    Returns a list of absolute directories.
    """
    results = []
    root_path = Path(root).resolve()
    for dirpath, dirnames, filenames in os.walk(root_path):
        # compute depth relative to root
        rel = Path(dirpath).resolve().relative_to(root_path)
        depth = len(rel.parts) if str(rel) != '.' else 0
        if depth > max_depth:
            # prevent deeper recursion
            # remove subdirs to stop os.walk from going deeper
            dirnames.clear()
            continue
        # look for known build markers
        if any(fname in filenames for fname in ("mvnw", "mvnw.cmd", "pom.xml", "gradlew", "gradlew.bat", "build.gradle")):
            results.append(os.path.abspath(dirpath))
    # deduplicate and sort for deterministic order
    unique = sorted(list(dict.fromkeys(results)))
    return unique


def choose_build_command(project_dir):
    """Return a list command to run to build the project, chosen by detected files and current OS."""
    p = Path(project_dir)
    files = {f.name for f in p.iterdir() if f.is_file()}
    is_windows = os.name == 'nt'

    # Prefer wrapper scripts if present
    if "mvnw" in files or "mvnw.cmd" in files or "pom.xml" in files:
        # prefer mvnw wrapper if exists
        if is_windows and "mvnw.cmd" in files:
            wrapper = str(p / "mvnw.cmd")
            cmd = [wrapper, "-DskipTests", "package"]
        elif not is_windows and "mvnw" in files:
            wrapper = str(p / "mvnw")
            cmd = [wrapper, "-DskipTests", "package"]
        else:
            # fallback to system mvn
            cmd = ["mvn", "-DskipTests", "package"]
        return cmd

    # Gradle projects
    if "gradlew" in files or "gradlew.bat" in files or "build.gradle" in files:
        if is_windows and "gradlew.bat" in files:
            wrapper = str(p / "gradlew.bat")
            cmd = [wrapper, "build", "-x", "test"]
        elif not is_windows and "gradlew" in files:
            wrapper = str(p / "gradlew")
            cmd = [wrapper, "build", "-x", "test"]
        else:
            cmd = ["gradle", "build", "-x", "test"]
        return cmd

    # Unknown: attempt to use mvn by default
    return ["mvn", "-DskipTests", "package"]


def build_application(app_dir):
    threads.append(app_dir)
    print(f"Building application at {app_dir}")
    cmd = choose_build_command(app_dir)
    print(f"Using build command: {' '.join(cmd)} (cwd={app_dir})")
    try:
        # Use subprocess.run for deterministic execution and capture output
        result = subprocess.run(cmd, cwd=app_dir, shell=False)
        if result.returncode == 0:
            print(f"Application {app_dir} finished building successfully!")
        else:
            print(f"Application {app_dir} failed to build. Return code: {result.returncode}")
    except FileNotFoundError as e:
        print(f"Build tool not found when building {app_dir}: {e}")
    except Exception as e:
        print(f"Unexpected error while building {app_dir}: {e}")
    finally:
        try:
            threads.remove(app_dir)
        except ValueError:
            pass


def docker_compose_up(compose_file_path=None):
    print("Running containers using docker-compose!")
    # allow passing a specific compose file if desired
    cmd = ["docker-compose", "up", "--build", "-d"]
    if compose_file_path:
        cmd = ["docker-compose", "-f", compose_file_path, "up", "--build", "-d"]
    try:
        subprocess.Popen(cmd)
        print("docker-compose launched in background.")
    except Exception as e:
        print(f"Failed to launch docker-compose: {e}")


def build_all_applications(root="."):
    print("Starting to discover and build applications!")
    roots = find_project_roots(root, max_depth=3)
    if not roots:
        print("No project roots found. Make sure projects contain a pom.xml, mvnw or build.gradle.")
        return

    print(f"Discovered {len(roots)} project(s):")
    for r in roots:
        print(f" - {r}")

    for app_path in roots:
        t = threading.Thread(target=build_application, args=(app_path,))
        t.start()


def remove_remaining_containers():
    print("Removing all containers.")
    try:
        subprocess.run(["docker-compose", "down"], check=False)
    except Exception as e:
        print(f"docker-compose down failed: {e}")

    try:
        out = subprocess.check_output(["docker", "ps", "-aq"], universal_newlines=True)
        containers = [c for c in out.splitlines() if c.strip()]
        if containers:
            print(f"There are still {len(containers)} containers created: {containers}")
            for container in containers:
                print(f"Stopping container {container}")
                subprocess.run(["docker", "container", "stop", container], check=False)
            subprocess.run(["docker", "container", "prune", "-f"], check=False)
    except Exception as e:
        print(f"Error while cleaning containers: {e}")


# python
if __name__ == "__main__":
    print("Pipeline started!")
    workspace_root = os.path.dirname(os.path.abspath(__file__))

    # discover and start build threads, keep Thread objects to join later
    roots = [
        os.path.join(workspace_root, "order-service", "order-service"),
        os.path.join(workspace_root, "orchestrator-service", "orchestrator-service"),
        os.path.join(workspace_root, "product-validation-service", "product-validation-service"),
        os.path.join(workspace_root, "payment-service", "payment-service"),
        os.path.join(workspace_root, "inventory-service", "inventory-service"),
    ]

    if not roots:
        print("No project roots found. Make sure projects contain a pom.xml, mvnw or build.gradle.")
    else:
        print(f"Discovered {len(roots)} project(s):")
        build_threads = []
        for r in roots:
            print(f" - {r}")
            t = threading.Thread(target=build_application, args=(r,))
            t.start()
            build_threads.append(t)

        # wait for builds to finish (join each thread)
        for t in build_threads:
            t.join()

    # after builds finished, bring up containers (docker-compose runs in background via Popen)
    docker_compose_up()
    print("Pipeline finished.")
