"""Sinh BuildInfo.java: kiểu bản build (dev = APK cài trực tiếp có mã mở khóa thử; store = bản Google Play) và mã sản phẩm."""
import hashlib, secrets, sys
from pathlib import Path

kind, out = sys.argv[1], Path(sys.argv[2])
root = Path(__file__).resolve().parent.parent
code_file = root / ".dev-code"
if kind == "dev":
    if not code_file.exists():
        code_file.write_text("".join(secrets.choice("0123456789") for _ in range(12)) + "\n", encoding="utf-8")
    code = code_file.read_text(encoding="utf-8").strip()
    digest = hashlib.sha256(("sotay-dev:" + code).encode()).hexdigest()
else:
    digest = ""
text = f'''/* Sinh tự động bởi buildtools/gen_buildinfo.py, đừng sửa tay. */
package vn.sotay.lichviet;

final class BuildInfo {{
    /** true: APK cài trực tiếp (có ô nhập mã mở khóa thử). false: bản Google Play (chỉ mở khóa bằng giao dịch). */
    static final boolean DEV = {"true" if kind == "dev" else "false"};
    static final String DEV_CODE_HASH = "{digest}";
    /** Mã sản phẩm "Bản Đầy Đủ" (mua một lần) tạo trong Play Console. */
    static final String PRODUCT_ID = "ban_day_du";

    private BuildInfo() {{
    }}
}}
'''
dest = out / "vn/sotay/lichviet/BuildInfo.java"
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(text, encoding="utf-8")
print(f"BuildInfo: {kind}")
