"""Chép giao diện web vào assets của APK, thay Google Fonts bằng phông chữ đóng gói sẵn để chạy không cần mạng."""
import re, shutil, sys
from pathlib import Path

src, fonts_root, out = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
FACES = {"be-vietnam-pro": ["400", "500", "600", "700"], "bricolage-grotesque": ["700", "800"]}
SUBSETS = ("vietnamese", "latin-ext", "latin")

(out / "fonts").mkdir(parents=True, exist_ok=True)
css = []
for face, weights in FACES.items():
    pkg = fonts_root / face / "package"
    for w in weights:
        text = (pkg / f"{w}.css").read_text(encoding="utf-8")
        for block in re.findall(r"/\*[^*]*\*/\s*@font-face\s*{[^}]*}", text):
            if not any(f"-{s}-{w}-normal" in block for s in SUBSETS):
                continue
            block = re.sub(r",\s*url\([^)]*\.woff\) format\('woff'\)", "", block)
            for f in re.findall(r"url\(\./files/([^)]+\.woff2)\)", block):
                shutil.copy(pkg / "files" / f, out / "fonts" / f)
            css.append(block.replace("./files/", ""))
(out / "fonts" / "fonts.css").write_text("\n".join(css) + "\n", encoding="utf-8")

html = src.read_text(encoding="utf-8")
html, n = re.subn(r'<link rel="preconnect"[^>]*>\s*<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>',
                  '<link rel="stylesheet" href="fonts/fonts.css">', html)
if n != 1:
    sys.exit("Không tìm thấy thẻ Google Fonts trong index.html")
(out / "index.html").write_text(html, encoding="utf-8")
print(f"web: {len(css)} font-face, index.html {len(html)//1024} KB")
