# Glory Menu (mod Fabric, nhiều phiên bản) — CHƯA được biên dịch thử

Mod nhỏ chạy phía client, đọc những gì launcher Glory ghi vào `config/glory-menu/`:

* `layout.json` (nếu bật "Dùng menu của tôi"): dời / đổi cỡ / đổi chữ / ẩn nút gốc, thêm nút thật, chữ, ảnh, ảnh động lên màn hình chính.
* `bg/` (hình nền, không phụ thuộc layout): `bg.json` + `f0.png, f1.png, …`. Mod vẽ hình nền (ảnh thường hoặc ảnh động) ở ScreenEvents.beforeRender,
  còn panorama gốc được làm trong suốt bằng gói `GloryWall.zip` (6 mặt panorama trong suốt, đặt cuối danh sách resource pack nên thắng mọi pack khác),
  nên hình nền lộ ra phía sau nút và logo. Từ 1.21.2 mọi màn hình menu (khi chưa vào thế giới) đều dùng panorama nên hình nền hiện ở tất cả;
  trước 1.21.2 chỉ màn hình chính dùng panorama nên hình nền chỉ hiện ở màn hình chính.

Không còn mixin vào renderer panorama, nên không có rủi ro sai chữ ký phương thức; chỉ có hai accessor (`ScreenAccessor`, `WidgetAccessor`).

## Phiên bản
Fabric 1.20 đến 1.21.5, mỗi mcX_Y_Z là một jar cho cả một khoảng (xem `mcdep` trong build.gradle và bảng MENU_RANGES trong cosmetics.js của launcher;
`node scripts/test-cos-ranges.js` kiểm tra hai bên khớp). Phần khác nhau giữa các phiên bản nằm gọn trong `adapters/<thời kỳ>/com/glory/menu/Compat.java`
(tạo Identifier, vẽ texture có màu, tạo texture từ PNG, kết nối server). Trước 1.20 (không có DrawContext) chưa hỗ trợ.

## Build
Dùng GitHub Actions "Build Glory Cosmetics" (xây cả mod cosmetic lẫn mod menu), tải artifact `ALL-glory-mods`, rồi chạy
`node scripts/install-cosmetics.js <thư mục chứa jar>` trong thư mục launcher. Launcher tự chép đúng jar vào thư mục mods của từng phiên bản.

## Nếu lỗi biên dịch
Mã viết theo tên Yarn nhưng chưa chạy được trong môi trường của mình. Chỗ dễ lệch tên nhất nằm trong `Compat.java`
(`ServerInfo`, `ConnectScreen.connect`, `DrawContext.drawTexture`, `NativeImageBackedTexture`) và `GloryMenuClient`
(`ClickableWidget.setAlpha`, `ctx.drawTooltip`, `ConfirmLinkScreen`). Gửi nguyên văn lỗi, thường sửa 1-2 dòng.

## Định dạng layout
Xem `Layout.java`. Toạ độ tính theo kích thước GUI đã scale (như `screen.width/height`), neo `left/center/right` x `top/middle/bottom`.
