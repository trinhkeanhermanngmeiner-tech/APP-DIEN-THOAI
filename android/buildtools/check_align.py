"""Kiểm tra các mục không nén trong APK đã được căn lề 4 byte."""
import struct, sys, zipfile

path = sys.argv[1]
raw = open(path, "rb").read()
bad = []
with zipfile.ZipFile(path) as z:
    for i in z.infolist():
        if i.compress_type != zipfile.ZIP_STORED:
            continue
        n, e = struct.unpack("<HH", raw[i.header_offset + 26:i.header_offset + 30])
        if (i.header_offset + 30 + n + e) % 4:
            bad.append(i.filename)
if bad:
    sys.exit("Chưa căn lề: " + ", ".join(bad))
print("căn lề: OK")
