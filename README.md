# 自动文件导入器

这是一个安全版安卓 App 项目，用于把用户授权的源文件夹中的文件导入到用户授权的目标文件夹。

## 功能

- 选择源文件夹
- 选择目标文件夹
- 一键导入源文件夹内的文件
- 支持递归复制子文件夹
- 目标文件已存在时自动跳过，避免重复覆盖
- 显示导入成功、跳过、失败日志
- 使用 Android 系统文件夹授权，不访问其他 App 私有目录，不绕过系统权限

## 使用方法

1. 用 Android Studio 打开 `AutoFileImporter` 文件夹。
2. 等待 Gradle 同步完成。
3. 连接安卓手机或启动模拟器。
4. 点击运行安装 App。
5. 在 App 内依次点击“选择源文件夹”和“选择目标文件夹”。
6. 点击“开始导入文件”。

## 项目结构

- `app/src/main/java/com/example/autofileimporter/MainActivity.java`：主界面和文件导入逻辑
- `app/src/main/AndroidManifest.xml`：App 配置
- `app/build.gradle`：安卓模块构建配置

## 注意

Android 10 及以上系统对文件访问限制较严格。本项目使用系统推荐的 Storage Access Framework，必须由用户手动选择并授权文件夹。

本项目不会，也不应该用于修改系统目录、绕过权限、写入其他 App 的私有数据目录或注入恶意文件。
