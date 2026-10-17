# 线性代数小双基

> 心算练习 —— 三阶行列式与矩阵相乘，带错题集与成绩统计。

一个纯 Java 手写的 Android 小应用，用来练线性代数的心算：屏幕上给一个 3×3 矩阵求行列式，或给两个矩阵求积矩阵，直接在心算后填结果。做错的题自动进入错题集，可以针对性地反复重做，直到真正掌握。

**不含 Gradle，不依赖任何第三方库**——构建脚本直接调用 Android SDK 的命令行工具。

<p align="center">
  <img src="res/mipmap-xxxhdpi/ic_launcher.png" alt="应用图标" width="140">
</p>

---

## 目录

- [这是什么](#这是什么)
- [功能](#功能)
- [技术说明](#技术说明)
- [构建](#构建)
- [验证与测试](#验证与测试)
- [项目结构](#项目结构)
- [工具脚本](#工具脚本)
- [常见问题](#常见问题)
- [版本](#版本)

---

## 这是什么

学线性代数的时候，行列式和矩阵乘法这两件事，理论看过一遍就会了，但**手算速度**只能靠反复练。市面上的刷题 App 大多绑定课程、广告多、或者题目类型固定。

这个小应用只做两件事，但把它做得足够顺手：

| 模式 | 题目 |
| --- | --- |
| **行列式计算** | 3×3 矩阵，求行列式的值 |
| **矩阵相乘** | 两个矩阵 A（m×k）与 B（k×n），求积矩阵 |

配套两个辅助系统：**错题集**（记录做错的题，可重做，追踪掌握状态）和**成绩统计**（分模式记录正确率与用时）。

## 功能

### 行列式计算

给出一个 3×3 矩阵，心算它的行列式，然后对照答案。

- **计时**：每道题单独计时，显示用时（精确到 0.1 秒）
- **查看答案**：不是直接甩一个数字，而是**逐步展开**计算过程（进度显示为 `n / 总步数`），方便排查是哪一步算错了
- **做错了**：点一下「❌ 做错了」把这题存入错题集

### 矩阵相乘

两个矩阵相乘，求积矩阵。出题前有一个**三步尺寸向导**：

1. **A 的行数** —— 先决定 A 有几行
2. **A 的列数 = B 的行数** —— 共享维，这一步决定了两个矩阵能否相乘
3. **B 的列数** —— 再决定 B 有几列

每一步都只在小范围里选，避免了一上来就要面对"填一堆维度"的压迫感。

尺寸覆盖 `m, k, n ∈ {2, 3, 4}`，共 **3 × 3 × 3 = 27 种组合**（A 从 2×2 到 4×4，积矩阵同样）。所以不只是练 2×2 和 3×3，能练到 4×4。

### 错题集

行列式和矩阵相乘的错题**分开记录**；矩阵相乘的错题再**按尺寸分组**（先选尺寸，再看该尺寸下的错题）。

错题集用的是**三态标记**，而不是简单的"做过/没做"：

| 状态 | 含义 | 怎么变成这样 |
| --- | --- | --- |
| ⬜ 白 | 还没重做过 | 题目刚存入 |
| 🟩 绿 | 已经掌握 | **重做做对一次**即变绿 |
| 🟥 红 | 还没掌握 | **重做又做错**，立刻变红 |

规则细节：

- 点击序号即可重做该题
- **绿色状态下再做错，会变红**
- **红色状态需要连续做对 2 次才能回到绿色** —— 避免"蒙对一次就当掌握"的自我欺骗
- 界面上显示「已掌握 X / Y」，随时知道还剩多少题

### 成绩统计

按模式（行列式 / 矩阵相乘）分别统计：

- 总次数
- **最近 N 次**的正确情况
- **最近 N 次的平均**表现

用滚动窗口而不是全部历史的平均值，这样最近练得好不好能立刻反映出来。

### 界面

- 主页只有两个入口（行列式计算 / 矩阵相乘），进去后一屏完成"出题 → 心算 → 核对"
- 半透明的照片背景（`bg_phone_img.webp`），不干扰读数
- 启动图标使用自定义美术素材

## 技术说明

### 不用 Gradle

构建完全依赖 Android SDK 自带的命令行工具，由 `build.ps1` 串起来：

```
aapt2 compile (编译资源)
    → aapt2 link (生成 base.apk + R.java)
    → javac     (编译 Java)
    → d8        (转成 classes.dex)
    → jar uf    (把 dex 塞回 apk)
    → zipalign  (4 字节对齐)
    → apksigner (v1 + v2 签名)
    → apksigner verify (校验)
```

**好处**：依赖极少（一个 JDK + SDK 就够），每一步发生了什么完全可见，出问题能定位到具体环节。
**代价**：没有增量构建，每次都要整包重建。

### 参数

| 项 | 值 |
| --- | --- |
| 语言 | Java（source / target 8） |
| 包名 | `com.example.detmatrix` |
| minSdk | 21（Android 5.0） |
| targetSdk | 34 |
| 版本 | 1.0（versionCode 1） |
| 第三方依赖 | 无 |

`build.ps1` 里用 `--min-sdk-version 21 --target-sdk-version 34 --version-code 1 --version-name "1.0"` 传给 aapt2，改版本就是改这几行。

## 构建

### 前置条件

**1. JDK 17**

脚本默认查找：

```powershell
$JDK = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
```

换成你自己的 JDK 17 路径。

**2. Android SDK**

需要以下两个组件：

| 组件 | 路径（相对 SDK 根） | 用途 |
| --- | --- | --- |
| build-tools 34.0.0 | `build-tools/34.0.0` | `aapt2`、`d8`、`zipalign`、`apksigner` |
| platform android-34 | `platforms/android-34/android.jar` | 编译时的 bootclasspath |

脚本默认：

```powershell
$SDK = "D:\android-sdk"
```

按实际位置修改。

> 如果还没装 SDK，最省事的办法是装 Android Studio 让它自动下载上述组件，然后删掉 Studio 也行——`build.ps1` 只用到 SDK 里的这几个命令行工具。

### 构建

```powershell
.\build.ps1
```

产物：**`Matrix.apk`**（已签名，可直接安装到手机或模拟器）。

首次构建时如果缺少 `debug.keystore`，脚本会用 `keytool` 自动生成一个调试签名（口令 `android`）。这个密钥**只用于本地调试，不要用于正式发布**。

### 安装到手机

```powershell
adb install -r Matrix.apk
```

或者把 `Matrix.apk` 传到手机上手动安装（需允许"安装未知来源应用"）。

## 验证与测试

### APK 校验

```powershell
.\verify.ps1
```

对已构建的 APK 做完整检查（签名状态、zip 对齐、清单信息等），构建脚本第 9 步也会自动跑一次 `apksigner verify --verbose --print-certs`。

### 单元测试

`test/MathTest.java` 覆盖行列式与矩阵乘法的计算逻辑，由 `build.ps1` 的 `testout` 步骤编译执行。

这一层测试的价值在于：**计分逻辑如果算错了，界面上看不出来**——你会以为自己做对了题却被判错。所以把纯计算部分单独抽出来测。

## 项目结构

```
Matrix/
├── AndroidManifest.xml             # 7 个 Activity 的声明
├── build.ps1                       # 构建脚本
├── verify.ps1                      # APK 校验套件
├── phonescreen.png                 # 背景图原始素材（tools/make_bg.py 的输入）
│
├── java/com/example/detmatrix/     # 12 个源文件
│   ├── MainActivity.java           # 主页：选择模式
│   ├── DetActivity.java            # 行列式练习（计时、查看答案、标记错题）
│   ├── MatMulActivity.java         # 矩阵相乘练习
│   ├── ShapeStepActivity.java      # 三步尺寸向导
│   ├── WrongListActivity.java      # 行列式错题集
│   ├── WrongShapesActivity.java    # 矩阵相乘错题集（按尺寸分组）
│   ├── WrongBook.java              # 错题本存储与三态逻辑
│   ├── StatsActivity.java          # 成绩统计界面
│   ├── StatsStore.java             # 统计数据存储
│   ├── Shapes.java                 # 矩阵尺寸空间（27 种组合）
│   ├── ElapsedTimer.java           # 计时器
│   └── Practice.java               # 练习数据模型
│
├── res/
│   ├── layout/                     # 7 个界面布局
│   ├── values/                     # strings / colors / 尺寸
│   ├── drawable*/                  # 背景、chip、图标前景
│   └── mipmap-*/                   # 各密度启动图标
│
├── test/MathTest.java              # 计算逻辑单元测试
└── tools/                          # 资源生成脚本（不参与运行时）
```

## 工具脚本

`tools/` 下是生成美术资源的 Python 脚本，**不属于运行时**，只是省得手工 PS：

| 脚本 | 用途 |
| --- | --- |
| `make_icons.py` | 生成各密度启动图标（含自适应图标的前景/背景层） |
| `make_bg.py` | 把根目录的 `phonescreen.png` 压成 `res/drawable-nodpi/bg_phone_img.webp`（半透明照片背景） |
| `preview_ui.py` | 基于同一张素材渲染 UI 预览图，改布局后先在这里看效果 |

**关于产物是否提交**：`preview_*.png` 被 `.gitignore` 排除（随时可重新生成）；但 `bg_phone_img.webp` **是提交进仓库的**——这样 `build.ps1` 只需要 JDK + Android SDK，**不必安装 Python 也能出包**。`phonescreen.png` 作为源素材同样进仓库，否则 `make_bg.py` 无法重跑。

## 常见问题

**Q：`build.ps1` 报 `aapt2 compile failed`**
A：多半是 `res/` 里有资源语法错误，或者 `$SDK` 路径指错了。先单独跑一次 `aapt2 compile --dir res -o test.zip` 看看具体报错。

**Q：报 `javac failed`，提示找不到符号**
A：检查 `$AJAR`（`platforms/android-34/android.jar`）是否存在——这个文件缺失时，所有 `android.*` 的引用都会找不到。

**Q：构建成功但装不上手机**
A：`minSdk` 是 21，设备系统版本低于 Android 5.0 会拒绝安装。另外 APK 用的是调试签名，和手机上已装的同包名应用签名不一致时，需要先卸载旧版。

**Q：仓库里为什么没有 APK？**
A：`.gitignore` 排除了 `*.apk`、`build/`、`debug.keystore` 和 `preview_*.png`。**构建产物不进版本库**——二进制的 APK 无法 diff/merge，而且每次重建都是全新的几百 KB，会永久膨胀历史。要分发 APK 请走 GitHub Releases。

**Q：为什么没有 Gradle？**
A：这个项目最初就是想看看"不靠 Gradle 能不能把 APK 构建出来"。答案是能，而且整条链路清晰可控。如果以后依赖变多、或者要做多 module，再迁移到 Gradle 也不迟。

## 版本

当前 **v1.0**（附注标签），功能集：行列式 + 矩阵相乘 + 错题集 + 统计。

## 许可

个人练习作品，仅供学习参考。如需复用代码请注明出处。
