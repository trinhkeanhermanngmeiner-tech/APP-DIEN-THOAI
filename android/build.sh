#!/usr/bin/env bash
# Đóng gói Sổ Tay Lịch Việt thành APK mà không cần Android Studio / Android SDK.
# Công cụ được tải về .tools/: android.jar (API 34), aapt2, dx, apksig và phông chữ.
# Kết quả: ../dist/SoTayLichViet.apk (cài trực tiếp) và ../dist/SoTayLichViet.aab (nộp lên Google Play)
# Tác giả: BS. Trịnh Kế An (bstrinhkean@gmail.com)
set -euo pipefail
cd "$(dirname "$0")"
ROOT=$(pwd); T=$ROOT/.tools; OUT=$ROOT/build; DIST=$ROOT/../dist
# Tên phiên bản giữ 1.0; mã phiên bản (VERSION_CODE) vẫn phải tăng mỗi lần phát hành để Android/Google Play cho cập nhật.
VERSION_CODE=${VERSION_CODE:-5}; VERSION_NAME=${VERSION_NAME:-1.0}; API=36

mkdir -p "$T"
fetch() { # url tên-tệp
  [ -s "$T/$2" ] && return 0
  for i in 1 2 3 4 5; do curl -fsSL -o "$T/$2" "$1" && return 0; sleep $((i * 3)); done
  echo "Không tải được $1" >&2; return 1
}
fetch https://raw.githubusercontent.com/Sable/android-platforms/master/android-$API/android.jar android-$API.jar
fetch https://repo.maven.apache.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/16.0.1/dalvik-dx-16.0.1.jar dx.jar
fetch https://github.com/google/bundletool/releases/download/1.18.1/bundletool-all-1.18.1.jar bundletool.jar
fetch https://repo.maven.apache.org/maven2/com/android/tools/build/apksig/2.3.0/apksig-2.3.0.jar apksig.jar
if [ ! -x "$T/aapt2" ]; then
  (cd "$T" && npm pack aaptjs3@2.0.2 --silent >/dev/null && tar xzf aaptjs3-2.0.2.tgz package/bin/x64/linux/aapt2 \
    && mv package/bin/x64/linux/aapt2 . && rm -rf package aaptjs3-2.0.2.tgz && chmod +x aapt2)
fi
for p in be-vietnam-pro bricolage-grotesque; do
  [ -d "$T/fonts/$p/package" ] || (mkdir -p "$T/fonts/$p" && cd "$T/fonts/$p" && npm pack "@fontsource/$p@5.3.0" --silent >/dev/null && tar xzf ./*.tgz && rm ./*.tgz)
done

rm -rf "$OUT"; mkdir -p "$OUT/assets/www" "$OUT/gen" "$OUT/classes" "$OUT/signer" "$OUT/bundle/base/manifest" "$OUT/bundle/base/dex" "$DIST"
python3 buildtools/prepare_web.py ../so-tay/index.html "$T/fonts" "$OUT/assets/www"

"$T/aapt2" compile --dir res -o "$OUT/res.zip"
"$T/aapt2" link -o "$OUT/base.apk" -I "$T/android-$API.jar" --manifest AndroidManifest.xml -A "$OUT/assets" \
  --java "$OUT/gen" --min-sdk-version 26 --target-sdk-version $API \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" --emit-ids "$OUT/ids.txt" "$OUT/res.zip"

javac -nowarn -Xlint:-options --release 8 -encoding UTF-8 -classpath "$T/android-$API.jar" -d "$OUT/classes" \
  $(find src "$OUT/gen" -name '*.java')
java -cp "$T/dx.jar" com.android.dx.command.Main --dex --min-sdk-version=26 --output="$OUT/classes.dex" "$OUT/classes"

cp "$OUT/base.apk" "$OUT/unaligned.apk"
(cd "$OUT" && zip -q unaligned.apk classes.dex)
python3 buildtools/zipalign.py "$OUT/unaligned.apk" "$OUT/aligned.apk"

KS=${KEYSTORE:-$ROOT/sotay-release.p12}; KS_PASS=${KEYSTORE_PASS:-sotaylichviet}
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storetype PKCS12 -storepass "$KS_PASS" -alias sotay \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=So Tay Lich Viet, O=So Tay, C=VN"
fi
javac -nowarn -encoding UTF-8 -classpath "$T/apksig.jar" -d "$OUT/signer" buildtools/Signer.java
java -Dstdout.encoding=UTF-8 --add-exports java.base/sun.security.x509=ALL-UNNAMED --add-exports java.base/sun.security.pkcs=ALL-UNNAMED --add-exports java.base/sun.security.util=ALL-UNNAMED -cp "$T/apksig.jar:$OUT/signer" Signer "$KS" "$KS_PASS" sotay "$OUT/aligned.apk" "$DIST/SoTayLichViet.apk"
python3 buildtools/check_align.py "$DIST/SoTayLichViet.apk"
"$T/aapt2" dump badging "$DIST/SoTayLichViet.apk" | grep -E "^(package|sdkVersion|targetSdkVersion|application-label):"

# ---- Gói .aab cho Google Play ----
"$T/aapt2" link --proto-format -o "$OUT/proto.zip" -I "$T/android-$API.jar" --manifest AndroidManifest.xml -A "$OUT/assets" \
  --min-sdk-version 26 --target-sdk-version $API --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  --stable-ids "$OUT/ids.txt" "$OUT/res.zip"
(cd "$OUT/bundle/base" && unzip -q ../../proto.zip && mv AndroidManifest.xml manifest/ && cp ../../classes.dex dex/ && zip -qr ../base.zip .)
rm -f "$DIST/SoTayLichViet.aab"
java -jar "$T/bundletool.jar" build-bundle --modules="$OUT/bundle/base.zip" --output="$DIST/SoTayLichViet.aab"
jarsigner -keystore "$KS" -storepass "$KS_PASS" -sigalg SHA256withRSA -digestalg SHA-256 "$DIST/SoTayLichViet.aab" sotay >/dev/null
jarsigner -verify "$DIST/SoTayLichViet.aab" | tail -1
# Kiểm tra: dựng lại APK từ .aab như Google Play sẽ làm
java -jar "$T/bundletool.jar" build-apks --bundle="$DIST/SoTayLichViet.aab" --output="$OUT/check.apks" --mode=universal \
  --aapt2="$T/aapt2" --ks="$KS" --ks-pass="pass:$KS_PASS" --ks-key-alias=sotay --key-pass="pass:$KS_PASS"
(cd "$OUT" && unzip -qo check.apks universal.apk) && "$T/aapt2" dump badging "$OUT/universal.apk" | grep -E "^(package|targetSdkVersion):" | sed 's/^/aab → /'
ls -la "$DIST"/SoTayLichViet.*
