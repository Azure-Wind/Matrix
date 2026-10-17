# 线性代数小双基

> 心算练习 —— 三阶行列式与矩阵相乘，带错题集与成绩统计。

一个纯 Java 编写的 Android 小应用，用来练线性代数的心算：给一个 3×3 矩阵求行列式，或给两个矩阵求积矩阵，直接在格子里填答案。做错的题自动进入错题集，可以针对性地重做。

![应用截图](phonescreen.png)

## 功能

### 行列式计算

给出一个 3×3 矩阵，心算它的行列式值。

### 矩阵相乘

在两个矩阵之间求积矩阵。出题前有一个**三步尺寸向导**：

1. 先定 A 的行数
2. 再定「A 的列数 = B 的行数」（共享维）
3. 最后定 B 的列数

这样覆盖了 **27 种**可能的尺寸组合，而不是固定几种。

### 错题集

按模式分开记录（行列式 / 矩阵相乘）。**三态**设计：

| 状态 | 含义 |
| --- | --- |
| 白 | 未重做 |
| 绿 | 重做做对了 |
| 红 | 重做又错了 |

规则：点击题号即可重做。**做对一次即变绿**；做错立刻变红，而**红色状态需要连续做对 2 次才能回到绿色**——避免运气好蒙对一次就当作已掌握。

### 成绩统计

练习次数、正确率等统计。

## 技术说明

**不使用 Gradle。** 构建完全依赖 Android SDK 自带的命令行工具，由 `build.ps1` 串起来：

```
aapt2 compile (资源) → aapt2 link (base.apk + R.java) → javac → d8 (dex)
  → jar 打包 classes.dex → zipalign → apksigner 签名
```

好处是依赖极少、构建链每一步都看得见；代价是没有增量构建，改动后要整包重建。

| 项 | 值 |
| --- | --- |
| 语言 | Java（source/target 8） |
| 包名 | `com.example.detmatrix` |
| minSdk | 21（Android 5.0） |
| targetSdk | 34 |
| 版本 | 1.0 (versionCode 1) |

## 构建

### 前置条件

- **JDK 17**（脚本默认查找 `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`，如不同请改 `build.ps1` 顶部的 `$JDK`）
- **Android SDK**，需要：
  - `build-tools/34.0.0`（用到 `aapt2`、`d8`、`zipalign`、`apksigner`）
  - `platforms/android-34/android.jar`
  - 脚本默认 `$SDK = "D:\android-sdk"`，按需修改

### 构建

```powershell
.\build.ps1
```

产物：`Matrix.apk`（已签名，可直接安装到手机或模拟器）。

首次构建时若缺少 `debug.keystore`，脚本会用 `keytool` 自动生成一个调试签名（口令 `android`，仅用于本地调试，**不要用于正式发布**）。

### 验证

```powershell
.\verify.ps1
```

对已构建的 APK 做完整校验（签名、对齐、清单信息等）。

### 单元测试

`test/MathTest.java` 覆盖行列式与矩阵乘法的计算逻辑，可由 `build.ps1` 的 `testout` 步骤编译执行。

## 项目结构

```
Matrix/
├── AndroidManifest.xml
├── build.ps1              # 构建脚本（aapt2 → javac → d8 → zipalign → apksigner）
├── verify.ps1             # APK 校验
├── java/com/example/detmatrix/
│   ├── MainActivity.java          # 主页：选择模式
│   ├── DetActivity.java           # 行列式练习
│   ├── MatMulActivity.java        # 矩阵相乘练习
│   ├── ShapeStepActivity.java     # 三步尺寸向导
│   ├── WrongListActivity.java     # 行列式错题集
│   ├── WrongShapesActivity.java   # 矩阵相乘错题集（按尺寸分组）
│   ├── WrongBook.java             # 错题本存储
│   ├── StatsActivity.java         # 成绩统计
│   ├── StatsStore.java            # 统计数据存储
│   ├── Shapes.java                # 矩阵尺寸模型
│   ├── ElapsedTimer.java          # 计时
│   └── Practice.java              # 练习数据
├── res/                    # 布局、图标、配色、背景图
├── test/MathTest.java      # 计算逻辑单元测试
└── tools/                  # 资源生成脚本（图标、背景、UI 预览）
```

## 工具脚本

`tools/` 下是用来生成美术资源的 Python 脚本（不属于运行时）：

| 脚本 | 用途 |
| --- | --- |
| `make_icons.py` | 生成各密度启动图标 |
| `make_bg.py` | 生成照片背景图 |
| `preview_ui.py` | 渲染 UI 预览图 |

## 版本

当前为 **v1.0**（附注标签）。按「行列式 + 矩阵相乘 + 错题集 + 统计」的功能集发布。

## 截图

`phonescreen.png` 为应用主界面截图。

---

本项目为个人练习作品，仅供学习参考。
