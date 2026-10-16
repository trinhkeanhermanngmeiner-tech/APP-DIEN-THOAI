"""Bản AndroidManifest cho Gradle: bỏ thuộc tính package (Gradle lấy namespace từ build.gradle)."""
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
s = (root / "AndroidManifest.xml").read_text(encoding="utf-8")
s, n = re.subn(r'\s+package="[^"]*"', "", s, count=1)
assert n == 1, "không tìm thấy thuộc tính package"
out = root / "build" / "gradle-manifest.xml"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(s, encoding="utf-8")
print("đã sinh", out.relative_to(root))
