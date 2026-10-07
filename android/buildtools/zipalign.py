"""Căn lề 4 byte cho các mục không nén trong APK (thay cho công cụ zipalign của Android SDK)."""
import struct, sys, zipfile

src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w") as zout:
    for info in zin.infolist():
        data = zin.read(info.filename)
        zi = zipfile.ZipInfo(info.filename, date_time=(2008, 1, 1, 0, 0, 0))
        zi.compress_type = info.compress_type
        zi.external_attr = info.external_attr
        if info.compress_type == zipfile.ZIP_STORED:
            base = zout.fp.tell() + 30 + len(zi.filename.encode("utf-8"))
            pad = (-(base + 6)) % 4
            zi.extra = struct.pack("<HHH", 0xD935, 2 + pad, 4) + b"\0" * pad
        zout.writestr(zi, data)
