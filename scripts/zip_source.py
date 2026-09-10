import os
import sys
import zipfile
import datetime
import shutil

def zip_project():
    # Root directory of the repository (parent of scripts/)
    script_dir = os.path.abspath(os.path.dirname(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))

    # Determine output directory (check user Downloads first, fallback to C:\Downloads)
    user_downloads = os.path.expanduser("~/Downloads")
    if os.path.exists(user_downloads):
        output_dir = user_downloads
    else:
        output_dir = r"C:\Downloads"
        os.makedirs(output_dir, exist_ok=True)

    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    zip_filename = f"simple_emc_source_{timestamp}.zip"
    zip_filepath = os.path.join(output_dir, zip_filename)
    latest_filepath = os.path.join(output_dir, "simple_emc_source.zip")

    # Directories to strictly exclude (build artifacts, caches, IDE data, VCS)
    exclude_dirs = {
        "build",
        ".gradle",
        ".git",
        ".vscode",
        ".agents",
        "bin",
        "out",
        "run",
        ".idea",
        "__pycache__",
        "dist",
        ".gemini"
    }

    # File extensions to exclude
    exclude_exts = {
        ".jar",
        ".class",
        ".log",
        ".pyc",
        ".pyo",
        ".swp",
        ".tmp"
    }

    print(f"Packaging source code from: {root_dir}")
    print(f"Excluding: build data, caches, compiled binaries, IDE/git data")

    file_count = 0
    with zipfile.ZipFile(zip_filepath, "w", zipfile.ZIP_DEFLATED) as zip_file:
        for dirpath, dirnames, filenames in os.walk(root_dir):
            # Prune excluded directories in-place
            dirnames[:] = [
                d for d in dirnames
                if d.lower() not in exclude_dirs and not d.startswith(".")
            ]

            for filename in filenames:
                ext = os.path.splitext(filename)[1].lower()
                if ext in exclude_exts or filename.startswith("."):
                    continue

                full_path = os.path.join(dirpath, filename)
                rel_path = os.path.relpath(full_path, root_dir)

                zip_file.write(full_path, rel_path)
                file_count += 1

    # Save a copy as simple_emc_source.zip for easy access
    shutil.copy2(zip_filepath, latest_filepath)

    zip_size_mb = os.path.getsize(zip_filepath) / (1024 * 1024)
    print("\n--- Package Successful ---")
    print(f"Total source files included: {file_count}")
    print(f"Archive size: {zip_size_mb:.2f} MB")
    print(f"Output files in Downloads:")
    print(f"  1. {zip_filepath}")
    print(f"  2. {latest_filepath}")

if __name__ == "__main__":
    zip_project()
