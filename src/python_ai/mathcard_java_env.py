import json
import os
import subprocess
from pathlib import Path


class MathCardJavaEnv:
    def __init__(self, project_root=None, auto_compile=True):
        self.project_root = self._find_project_root(project_root)
        self.bin_dir = self.project_root / "bin"
        self.json_jar = self.project_root / "lib" / "json-20240303.jar"

        if auto_compile:
            self._compile_java_if_needed()

        classpath = os.pathsep.join([
            str(self.bin_dir),
            str(self.json_jar),
        ])

        self.process = subprocess.Popen(
            ["java", "-cp", classpath, "ai_access.EnvServer"],
            cwd=self.project_root,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
        )

    @staticmethod
    def _find_project_root(project_root):
        if project_root is not None:
            root = Path(project_root).expanduser().resolve()
            MathCardJavaEnv._validate_project_root(root)
            return root

        for candidate in Path(__file__).resolve().parents:
            env_server = candidate / "src" / "main" / "ai_access" / "EnvServer.java"
            json_jar = candidate / "lib" / "json-20240303.jar"
            if env_server.exists() and json_jar.exists():
                return candidate

        raise FileNotFoundError(
            "Could not find the MathCard project root. Pass project_root explicitly."
        )

    @staticmethod
    def _validate_project_root(root):
        env_server = root / "src" / "main" / "ai_access" / "EnvServer.java"
        json_jar = root / "lib" / "json-20240303.jar"
        if not env_server.exists():
            raise FileNotFoundError(f"Missing EnvServer.java under project root: {root}")
        if not json_jar.exists():
            raise FileNotFoundError(f"Missing json-20240303.jar under project root: {root}")

    def _compile_java_if_needed(self):
        env_server_class = self.bin_dir / "ai_access" / "EnvServer.class"
        source_root = self.project_root / "src" / "main"
        java_sources = list(source_root.rglob("*.java"))

        if not java_sources:
            raise FileNotFoundError(f"No Java source files found under: {source_root}")

        if env_server_class.exists():
            newest_source_time = max(source.stat().st_mtime for source in java_sources)
            if env_server_class.stat().st_mtime >= newest_source_time:
                return

        self.bin_dir.mkdir(exist_ok=True)
        command = [
            "javac",
            "-cp",
            str(self.json_jar),
            "-d",
            str(self.bin_dir),
            *[str(source) for source in java_sources],
        ]

        try:
            result = subprocess.run(
                command,
                cwd=self.project_root,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
        except FileNotFoundError as error:
            raise RuntimeError(
                "Could not find javac. Install a JDK or compile the project in VS Code first."
            ) from error

        if result.returncode != 0:
            raise RuntimeError(
                "javac failed.\n"
                f"stdout:\n{result.stdout}\n"
                f"stderr:\n{result.stderr}"
            )

    def reset(self):
        return self._send({"cmd": "reset"})

    def step(self, action):
        return self._send({"cmd": "step", "action": action})

    def close(self):
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                self.process.kill()

    def _send(self, request):
        if self.process.poll() is not None:
            stderr = self.process.stderr.read()
            raise RuntimeError(f"EnvServer already stopped. stderr: {stderr}")

        self.process.stdin.write(json.dumps(request) + "\n")
        self.process.stdin.flush()

        line = self.process.stdout.readline()
        if not line:
            stderr = self.process.stderr.read()
            raise RuntimeError(f"EnvServer returned no data. stderr: {stderr}")

        response = json.loads(line)
        if not response.get("ok", False):
            raise RuntimeError(response.get("error", "Unknown EnvServer error"))

        return response

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()


if __name__ == "__main__":
    with MathCardJavaEnv() as env:
        result = env.reset()
        observation = result["observation"]
        print("EnvServer is working.")
        print(f"State: {observation['state']}")
        print(f"Legal actions: {sum(result['actionMask'])}")
