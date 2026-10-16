#!/usr/bin/env bash
# Chuẩn bị tài nguyên dùng chung cho mọi kiểu build: phông chữ, tài nguyên sinh tự động, giao diện web, BuildInfo.
# Cách dùng: ./prepare.sh dev|store   (dev: APK cài trực tiếp có mã mở khóa thử; store: bản Google Play)
# Tác giả: BS. Trịnh Kế An (bstrinhkean@gmail.com)
set -euo pipefail
cd "$(dirname "$0")"
KIND=${1:-dev}; T=$(pwd)/.tools; OUT=$(pwd)/build
case "$KIND" in dev|store) ;; *) echo "Dùng: $0 dev|store" >&2; exit 1;; esac

mkdir -p "$T"
for p in be-vietnam-pro bricolage-grotesque; do
  [ -d "$T/fonts/$p/package" ] || (mkdir -p "$T/fonts/$p" && cd "$T/fonts/$p" && npm pack "@fontsource/$p@5.3.0" --silent >/dev/null && tar xzf ./*.tgz && rm ./*.tgz)
done
python3 buildtools/gen_month_widget.py >/dev/null
python3 buildtools/gen_skins.py >/dev/null
rm -rf "$OUT"; mkdir -p "$OUT/assets/www" "$OUT/buildinfo"
python3 buildtools/prepare_web.py ../so-tay/index.html "$T/fonts" "$OUT/assets/www"
python3 buildtools/gen_buildinfo.py "$KIND" "$OUT/buildinfo"
python3 buildtools/gen_gradle_manifest.py
