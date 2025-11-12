# Git Guide

## Lần đầu push lên GitHub

### 1. Tạo repository mới trên GitHub
- Truy cập https://github.com/new
- Đặt tên: `game-xep-hang` (hoặc tên khác)
- Chọn Public hoặc Private
- **KHÔNG** chọn "Initialize with README" (vì đã có sẵn)
- Click "Create repository"

### 2. Initialize Git local

```bash
cd c:\Users\ASUS\Desktop\xep_hang_test\GameXepHang_test2

# Khởi tạo git
git init

# Thêm tất cả file
git add .

# Commit đầu tiên
git commit -m "Initial commit: Game Xếp Hàng MVC Structure"
```

### 3. Push lên GitHub

```bash
# Thêm remote repository (thay YOUR_USERNAME và YOUR_REPO)
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git

# Push lên main branch
git branch -M main
git push -u origin main
```

## Workflow thông thường

### Mỗi lần thay đổi code

```bash
# 1. Kiểm tra file đã thay đổi
git status

# 2. Thêm file muốn commit
git add .

# 3. Commit với message mô tả
git commit -m "Fix: Sửa lỗi kết nối database"

# 4. Push lên GitHub
git push
```

### Pull code mới từ GitHub

```bash
git pull origin main
```

## Git Commands hữu ích

```bash
# Xem lịch sử commit
git log --oneline

# Xem thay đổi chưa commit
git diff

# Hoàn tác thay đổi file
git checkout -- filename

# Xem remote URL
git remote -v

# Đổi remote URL
git remote set-url origin NEW_URL
```

## Branch Strategy

```bash
# Tạo branch mới cho feature
git checkout -b feature/lobby-improvement

# Chuyển về main
git checkout main

# Merge branch
git merge feature/lobby-improvement

# Xóa branch sau khi merge
git branch -d feature/lobby-improvement
```

## .gitignore đã setup

File `.gitignore` đã loại trừ:
- ✅ File `.class` compiled
- ✅ Thư mục `bin/`, `out/`, `build/`
- ✅ File IDE (`.idea`, `.vscode`)
- ✅ File backup (`src/backup/`)
- ✅ Log files
- ✅ OS files (Thumbs.db, .DS_Store)

## Lưu ý quan trọng

⚠️ **KHÔNG** commit:
- Mật khẩu database thật
- File `.class` compiled
- Thư mục `bin/`
- Thông tin cá nhân

✅ **NÊN** commit:
- Source code (`.java`)
- File config mẫu (`DatabaseConfig.java`)
- Documentation (`.md`)
- SQL schema (`database/schema.sql`)
