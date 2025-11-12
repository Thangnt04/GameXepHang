# Cấu trúc mục tiêu

```
GameXepHang_test2/
├─ src/
│  ├─ models/       # dữ liệu miền, entity, value object, DTO
│  ├─ views/        # hiển thị UI, scene, prefab view, renderer
│  ├─ controllers/  # điều phối luồng, nhận input, gọi service, cập nhật view
│  ├─ services/     # nghiệp vụ thuần, không phụ thuộc UI
│  ├─ repositories/ # truy cập dữ liệu, file/io, playerprefs, api (tùy chọn)
│  ├─ utils/        # helper, extension, constants
│  ├─ events/       # event, message, pub/sub
│  ├─ styles/       # css/scss (web) hoặc style config
│  └─ tests/        # test
├─ assets/
│  ├─ images/       # png, jpg, svg, ...
│  ├─ audio/        # mp3, wav, ogg
│  └─ fonts/        # ttf, otf, woff
├─ config/          # config json/yaml
├─ data/            # csv, txt, mock data
├─ docs/            # tài liệu
└─ tools/           # script tiện ích (ví dụ restructure.ps1)
```

Quy tắc phân loại file (mặc định)
- Mã nguồn: .cs, .js, .jsx, .ts, .tsx, .py → src/ và đặt theo MVC:
  - controllers/: xử lý input, điều phối service → view
  - models/: dữ liệu miền, trạng thái, DTO
  - views/: render/hiển thị, bind dữ liệu, không chứa nghiệp vụ
  - services/: nghiệp vụ, thuật toán, validator
  - repositories/: truy cập dữ liệu ngoài (tùy chọn)
  - utils/: tiện ích chung
- Styles: .css, .scss → src/styles/
- Ảnh: .png, .jpg, .jpeg, .gif, .svg → assets/images/
- Âm thanh: .mp3, .wav, .ogg → assets/audio/
- Font: .ttf, .otf, .woff, .woff2 → assets/fonts/
- Cấu hình: .json, .yaml, .yml → config/
- Dữ liệu: .csv, .txt → data/
- Tài liệu: .md → docs/ (trừ chính file này)
- Bỏ qua thư mục hệ thống/build: .git, node_modules, bin, obj, build, dist, .vs

## Quy ước tách file và đặt tên

- Một lớp/một trách nhiệm/một file. Tránh “god file”.
- Tên kèm hậu tố:
  - XxxController, XxxService, XxxRepository, XxxModel, XxxView
- Controller không chứa nghiệp vụ; gọi Service và cập nhật View.
- View không biết Service trực tiếp (qua Controller hoặc binding).
- Model không tham chiếu View/Controller.
- Service thuần (test dễ), không dùng API UI.
- Tách sự kiện sang src/events (XxxCreated, ScoreUpdated, ...).
- Hạn chế file > 300–400 dòng; tách phương thức lớn thành private helper.

## Lộ trình tách (gợi ý nhanh)

1) Xác định “god files” (file lớn, nhiều trách nhiệm).
2) Tách Model: cấu trúc dữ liệu, trạng thái → src/models.
3) Tách View: mã render/UI → src/views.
4) Tách Controller: xử lý input, gọi service, bắc cầu model↔view → src/controllers.
5) Tách Service: tính toán/xử lý nghiệp vụ → src/services.
6) Di chuyển IO/đọc ghi dữ liệu → src/repositories.
7) Đổi tên theo hậu tố chuẩn; cập nhật import/namespace.
8) Viết/di chuyển test tương ứng → src/tests.

## Ví dụ khung tách (rút gọn)

- Trước: GameManager.cs (gộp input + logic + render)
- Sau:
  - controllers/GameController.cs: nhận input, điều phối
  - services/ScoreService.cs: tính điểm/thắng
  - models/ScoreModel.cs: dữ liệu điểm/trạng thái
  - views/GameBoardView.cs: vẽ bàn chơi, update UI

## Cách chạy script

- Dry-run (không thay đổi file):
  powershell -ExecutionPolicy Bypass -File .\tools\restructure.ps1

- Thực thi di chuyển file:
  powershell -ExecutionPolicy Bypass -File .\tools\restructure.ps1 -Execute

## Lưu ý sau khi di chuyển

- Cập nhật lại đường dẫn import/namespace, tài nguyên (ảnh/âm thanh/phông).
- Script bỏ qua: .git, node_modules, bin, obj, build, dist, .vs, và cả tools, docs.
- Nếu trùng tên file ở thư mục đích, script tự động thêm hậu tố -moved để tránh ghi đè.
- Có thể hoàn tác thủ công bằng log di chuyển trên console.
