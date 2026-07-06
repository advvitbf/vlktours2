import os

Import("env")

toolchain_bin = os.path.expanduser(
    r"~\.platformio\packages\toolchain-riscv32-esp\bin"
)

if os.path.isdir(toolchain_bin):
    system_paths = [
        os.path.expandvars(r"%SystemRoot%\System32"),
        os.path.expandvars(r"%SystemRoot%"),
    ]
    current_path = env["ENV"].get("PATH", os.environ.get("PATH", ""))
    forced_path = os.pathsep.join([toolchain_bin, *system_paths, current_path])
    env["ENV"]["PATH"] = forced_path
    env["ENV"]["PATHEXT"] = ".COM;.EXE;.BAT;.CMD"
    os.environ["PATH"] = forced_path
    os.environ["PATHEXT"] = env["ENV"]["PATHEXT"]
    env.PrependENVPath("PATH", toolchain_bin)
    env.Replace(
        CC=os.path.join(toolchain_bin, "riscv32-esp-elf-gcc.exe"),
        CXX=os.path.join(toolchain_bin, "riscv32-esp-elf-g++.exe"),
        AR=os.path.join(toolchain_bin, "riscv32-esp-elf-ar.exe"),
        RANLIB=os.path.join(toolchain_bin, "riscv32-esp-elf-ranlib.exe"),
        OBJCOPY=os.path.join(toolchain_bin, "riscv32-esp-elf-objcopy.exe"),
        SIZE=os.path.join(toolchain_bin, "riscv32-esp-elf-size.exe"),
    )
