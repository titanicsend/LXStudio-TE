rm -rf TE.iconset
mkdir TE.iconset
sips -z 16 16     te.png --out TE.iconset/icon_16x16.png
sips -z 32 32     te.png --out TE.iconset/icon_16x16@2x.png
sips -z 32 32     te.png --out TE.iconset/icon_32x32.png
sips -z 64 64     te.png --out TE.iconset/icon_32x32@2x.png
sips -z 128 128   te.png --out TE.iconset/icon_128x128.png
sips -z 256 256   te.png --out TE.iconset/icon_128x128@2x.png
sips -z 256 256   te.png --out TE.iconset/icon_256x256.png
sips -z 512 512   te.png --out TE.iconset/icon_256x256@2x.png
sips -z 512 512   te.png --out TE.iconset/icon_512x512.png
cp te.png TE.iconset/icon_512x512@2x.png
iconutil -c icns TE.iconset
rm -R TE.iconset
