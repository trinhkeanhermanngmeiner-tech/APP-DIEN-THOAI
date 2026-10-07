#!/usr/bin/env bash
# Kiểm tra mở ứng dụng trên Android 8 → 16 bằng Robolectric (chạy mã Android thật trên máy tính).
# Chạy sau android/build.sh. Tác giả: BS. Trịnh Kế An
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p target
sed 's#package="vn.sotay.lichviet">#package="vn.sotay.lichviet">\n    <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36" />#' ../AndroidManifest.xml > target/merged-manifest.xml
mvn -B -q test 2>&1 | grep -v JAVA_TOOL_OPTIONS | grep -E "OK sdk|Tests run|ERROR|Exception|^\s+at vn\." || true
grep -q 'errors="0"' target/surefire-reports/TEST-vn.sotay.lichviet.LaunchTest.xml && grep -q 'failures="0"' target/surefire-reports/TEST-vn.sotay.lichviet.LaunchTest.xml && echo "KẾT QUẢ: mở app thành công trên mọi phiên bản Android đã thử"
