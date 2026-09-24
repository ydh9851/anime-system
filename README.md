# 🎌 番析 AniScope —— 动漫数据分析与推荐系统

> 基于 MyAnimeList 动漫数据集（anime-dataset-2023.csv，2.4 万+ 部）与用户评分数据，构建覆盖「数据清洗 → 随机抽样 → 数据探索（EDA）→ 热度预测 → 个性化推荐 → Web 展示」全流程的动漫数据分析平台，采用 Java 后端 + Python 算法引擎的跨语言架构。

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green) ![Vue](https://img.shields.io/badge/Vue-3-brightgreen) ![Vite](https://img.shields.io/badge/Vite-6-purple) ![Python](https://img.shields.io/badge/Python-3.12-blue) ![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1) ![Redis](https://img.shields.io/badge/Redis-Session%20Cache-red) ![License](https://img.shields.io/badge/License-MIT-yellow)

**核心功能一览**

| 模块 | 说明 | 技术要点 |
| --- | --- | --- |
| 📈 动漫属性分析 | 类型 / 语言 / 年份 / 热度分布可视化 | ECharts / 后端统计接口 |
| 📊 动漫数据探索（EDA） | 22 张中文分析图与词云：评分 / 题材 / 热度 / 年份 / 制作公司等 | pandas / matplotlib / seaborn / wordcloud |
| 🔥 热度预测 | 6 种机器学习模型（LR / KNN / SVR / DT / RF / LightGBM）+ 对数变换特征工程 | LightGBM / scikit-learn |
| 🧠 深度学习推荐 | NCF 神经协同过滤（NeuMF：GMF + MLP 融合） | PyTorch |
| 🎯 动漫推荐 | 10 种算法：人口统计 / 内容 / 关键词 / User-KNN / Item-KNN / SVD / **NCF** / 集成等 | scikit-surprise / PyTorch |
| 🗄️ 数据管理 | 动漫库、用户、评分、播放量后台管理 | MyBatis-Plus / MySQL 8 |
| 🖥️ 管理界面 | Vue 3 单页应用，后端一体化托管 | Vue 3 / Element Plus / ECharts |

**设计文档**

| 文档 | 说明 |
| --- | --- |
| [01-需求分析说明书](docs/01-需求分析说明书.md) | 项目背景、数据画像、功能/非功能需求、需求追踪矩阵 |
| [02-概要设计说明书](docs/02-概要设计说明书.md) | 系统架构、技术选型、模块划分、数据库与接口设计 |
| [03-算法设计说明书](docs/03-算法设计说明书.md) | 12 种算法（6 种机器学习 + NCF 深度学习 + 5 类推荐）原理与设计 |
| [04-文档检查报告](docs/04-文档检查报告.md) | 文档符合性、数据准确性、一致性检查与结论 |
| [05-系统详细设计说明书](docs/05-系统详细设计说明书.md) | 数据库表/接口/页面/算法契约到字段级详细设计 |
| [06-联调测试报告](docs/06-联调测试报告.md) | Web 与底层算法联调测试过程与结论 |

#### 介绍

随着动漫产业数字化发展，MyAnimeList（MAL）与 Kaggle 等平台积累了海量动漫元数据与用户行为数据，而传统的人工运营推荐方式效率低、颗粒度粗，难以满足用户个性化追番需求。基于数据挖掘与机器学习的内容分析、热度预测与个性化推荐，已成为传媒数据应用的核心环节。

本项目从零构建一套完整的动漫数据分析与推荐系统，涵盖数据清洗、特征工程、数据可视化、热度预测建模、个性化推荐算法、后端 API、前端界面与数据库设计，并实现 Java 后端与 Python 算法引擎的跨语言调用，模拟企业级开发全流程。

#### 软件架构

系统按企业级开发规范组织，各需求对应的底层算法模块相互独立、共享同一份数据集与清洗产物：

```text
anime-system/                          # 系统根目录（整个目录拷贝到任何机器即可部署，零外部依赖）
├── .venv/                             # Python 3.12 虚拟环境（算法依赖已装好；不可用时可运行 setup_venv.cmd 重建）
├── data/                              # 统一数据集目录（工程唯一数据根，各算法模块共享）
│   ├── raw/
│   │   ├── anime-dataset-2023.csv     # 原始动漫数据集（24905 部作品，含多语言名称）
│   │   ├── kaggle_anime.csv           # Kaggle 原始动漫元数据（备用）
│   │   └── kaggle_rating.csv          # Kaggle 原始评分数据（备用）
│   ├── processed/                     # 清洗与抽样产物
│   │   ├── anime_train.csv            # 标准化训练集（5000 条，26 个字段）
│   │   ├── anime_test.csv             # 标准化测试集（5000 条）
│   │   └── data_report.md             # 数据初析报告（缺失值 / 分布 / 清洗决策）
│   ├── train.csv                      # 热度预测训练集（5000 条）
│   ├── test.csv                       # 热度预测测试集（5000 条）
│   └── recommendation/                # 推荐数据：动漫元数据 + 用户评分
│       ├── anime_5000.csv
│       ├── anime_5000_credits.csv
│       └── personal/                  # 个人化数据（anime_titles/ratings/links + 划分 train/test）
├── algorithm/                         # Python 算法引擎（不含数据，统一从 data/ 读取）
│   ├── eda/                           #   动漫数据探索（eda_anime.py + wordcloud_anime.py + figures/ 22 张图）
│   ├── prediction/                    #   动漫热度预测（predict_api.py + 6 种模型 + model_cache）
│   └── recommendation/                #   动漫推荐（recommend_api.py + 10 种算法）
│       ├── naive_recommender/         #     人口统计 / 内容相似 / 关键词 TF-IDF
│       ├── personal_recommender/      #     用户KNN / 物品KNN / SVD
│       ├── ensemble_recommender/      #     3 种集成推荐
│       └── deep_recommender/          #     深度学习 NCF（PyTorch NeuMF）
├── docs/                              # 设计文档（需求分析 / 概要设计 / 算法设计 / 检查报告）
├── scripts/                           # 数据构建脚本
│   └── data_prep/
│       └── build_anime_dataset.py     # 清洗 + 分层抽样 + 生成 train/test
├── sql/                               # 数据库建库 / 修复 / 数据导入脚本
├── backend/                           # Spring Boot 后端（内嵌编译好的前端页面，端口 8000）
├── frontend/                          # Vue 前端源码（构建产物位于 backend/src/main/resources/static/admin）
├── requirements.txt                   # Python 依赖清单
└── setup_venv.cmd                     # 新机器一键重建 Python 虚拟环境
```

**数据目录规范（重要）**

- 工程内**只有 `data/` 一个数据根**，任何算法模块下都不再自带 `data` 目录（历史重复副本已清理）；
- 算法脚本统一用「自身文件位置」推导系统根目录后再拼接数据路径（`SYSTEM_ROOT/data/...`），
  不依赖当前工作目录，因此在任意目录下执行脚本都能正确读到数据；
- 目录划分：`data/raw/` 存放原始动漫数据集，`data/processed/` 存放清洗与抽样产物，`data/train.csv`、`data/test.csv` 为热度预测所需的 5000 条数据集，`data/recommendation/` 放推荐所需的动漫元数据与用户评分（含随机划分出的 `personal/train|test.csv`）。

#### 数据集构建与 EDA

```bash
# 1. 数据清洗 + 分层随机抽样（生成 5000 训练 / 5000 测试）
.venv\Scripts\python.exe scripts/data_prep/build_anime_dataset.py

# 2. 生成 19 张中文 EDA 图表（输出到 algorithm/eda/figures/）
.venv\Scripts\python.exe algorithm/eda/eda_anime.py
```

- 数据源：`data/raw/anime-dataset-2023.csv`（24905 部动漫作品，含日文/英文/其他语言名称）
- 清洗规则：剔除评分缺失、成员数为 0、类型缺失的记录；按 `anime_id` 去重
- 抽样方式：按动漫类型（TV / Movie / OVA / ONA / Special / Music）分层随机抽样，`random_state=42`
- 字段映射：`budget`=集数、`revenue`=成员数（热度）、`popularity`=收藏数、`runtime`=单集时长、`status`=动漫类型
- 详细分析见 `data/processed/data_report.md`


#### 部署教程（整个目录拷贝到新机器即可，零外部代码依赖）

> 本工程已自包含：算法代码、数据集、前端页面、Python 虚拟环境全部收编在
> `anime-system` 目录内；`backend/src/main/resources/application-dev.yml` 中 Python
> 相关路径均为「相对 backend 目录」的相对路径，因此拷贝到任何盘符 / 目录都**无需改配置**。

**目标机器环境要求（基础软件，与外部代码无关）**

| 软件   | 版本            | 用途                         |
| ------ | --------------- | ---------------------------- |
| JDK    | 17（必须） | 后端。⚠️ 不要用 21/25+：Spring Session Redis 用 Java 原生序列化存登录态，新 JDK 反序列化不兼容会导致「主界面全 0 / 我的追番报错」|
| Maven  | 3.9+            | 后端构建                     |
| MySQL  | 8.x             | 数据库（库名 anime_mangage_db）|
| Redis  | 5+（127.0.0.1:6379，默认无密码） | 会话缓存 |
| Python | 3.10~3.12（仅在重建虚拟环境时需要） | 算法引擎 |

**部署步骤**

1. 拷贝整个 `anime-system` 文件夹到目标机器（路径随意，推荐纯英文路径）。
2. 启动 MySQL 与 Redis 服务。
3. 初始化数据库（建库建表，Windows 在 mysql bin 目录或配好 PATH 后执行）：
   ```bash
   mysql -uroot -p123456 < sql/anime_mangage_db.sql
   ```
   > 若 `t_video_info` 字段缺失，可再执行 `sql/_recreate_video_info.sql` 重建该表。
4. 把 Kaggle 动漫数据导入业务库：
   ```bash
   .venv\Scripts\python.exe sql/import_data.py --password 123456
   ```
   > 数据源在统一目录 `data/recommendation`（动漫元数据 + 评分），已随包携带，不依赖任何外部目录。
5. Python 虚拟环境：本包已随带 `.venv`（Python 3.12）。若换机器后不可用
   （例如目标机未安装同路径 Python 3.12），在 `anime-system` 根目录**双击 `setup_venv.cmd`** 即可自动重建。
6. 启动后端（工作目录必须是 `backend`）：
   - **推荐**：双击项目根目录 `restart_backend.cmd`（脚本已内置 JDK 17 自动选择，不需要手动配 JAVA_HOME）
   - 命令行：`cd backend && mvn spring-boot:run`
   - 或打包后运行：`mvn clean package` 后 `java -jar target/animeAnalysisSystem-3.0.3.jar`
   - IDE：用 IDE 打开 `backend`（Maven 工程），在 Project Structure 里把 SDK / Maven JRE 设为 **JDK 17**，再运行主类 `com.alvis.media.MediaApplication`
7. 浏览器访问 `http://localhost:8000/admin`，登录 `admin / 123456`。

#### 服务启停与重启（Windows / PowerShell）

> **懒人版**：双击项目根目录的 **`restart_backend.cmd`** 即可一次完成
> 「JDK 17 检测 → 查进程 → 杀进程 → 启动 → 等待端口 → 健康检查」。
> 脚本自动优先使用 `C:\developtools\jdk17`，找不到时回退系统 `JAVA_HOME`，
> 与电脑上其他项目的 JDK 互不影响。
>
> 前端页面由后端一并托管（`/admin`），**日常使用只需启停后端一个进程**；
> 只有在改前端源码做开发调试时才需要另开 `cd frontend && npm run dev`（端口 8002，已代理到 8000）。

**0. 前置中间件自检**

```powershell
netstat -ano | findstr :3306 | findstr LISTENING   # MySQL
netstat -ano | findstr :6379 | findstr LISTENING   # Redis
```

**1. 检查当前后端进程**（后端端口 8000）

```powershell
netstat -ano | findstr :8000 | findstr LISTENING
```

**2. 停止后端**

```powershell
$p = (Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue).OwningProcess
if ($p) { Stop-Process -Id $p -Force }
```

**3. 启动后端**

```powershell
cd backend
mvn -q -Dmaven.test.skip=true compile spring-boot:run   # 前台运行，Ctrl+C 结束

# 后台运行（关掉窗口也不中断，日志写入项目根目录 _boot.log）
Start-Process cmd.exe -ArgumentList '/c','cd /d backend && mvn -q -Dmaven.test.skip=true compile spring-boot:run 1> ..\_boot.log 2>&1' -WindowStyle Hidden
```

> **为什么要加 `-Dmaven.test.skip=true`**：`spring-boot:run` 会先执行 `test-compile`，而 `src/test`
> 下残留 JUnit4 写法（`@RunWith`、`org.junit.Test`），Spring Boot 3.5 的 `spring-boot-starter-test`
> 只带 JUnit5，会在编译测试源码时直接报错中断。因此启动与打包统一跳过测试编译。

**4. 健康检查**

```powershell
# 4.1 端口探针
$c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',8000); $c.Close(); echo 'port 8000 OK'

# 4.2 页面可用性（返回 200）
(Invoke-WebRequest -Uri 'http://127.0.0.1:8000/admin/index.html' -UseBasicParsing).StatusCode

# 4.3 算法链路（返回 1 说明 Python 引擎调用正常）
(Invoke-RestMethod -Uri 'http://127.0.0.1:8000/api/recommend' -Method Post -ContentType 'application/json' -Body '{"algo":"demographic","top":3}').code

# 4.4 查看运行日志
Get-Content _boot.log -Tail 30
```

**5. 一键重启（推荐日常使用）**：杀掉旧进程 → 重新启动 → 轮询等待端口 → 自动健康检查

```powershell
$old = netstat -ano | findstr ':8000' | findstr LISTENING | ForEach-Object { ($_ -split '\s+')[-1] }
foreach ($o in ($old | Select-Object -Unique)) { if ($o) { taskkill /PID $o /F } }
Start-Sleep -Seconds 2
Start-Process cmd.exe -ArgumentList '/c','cd /d backend && mvn -q -Dmaven.test.skip=true compile spring-boot:run 1> ..\_boot.log 2>&1' -WindowStyle Hidden
for ($i=0; $i -lt 120; $i++) {
  Start-Sleep -Seconds 3
  try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',8000); $c.Close()
        Write-Output "BACKEND UP after $(($i+1)*3)s"; break } catch {}
}
(Invoke-WebRequest -Uri 'http://127.0.0.1:8000/admin/index.html' -UseBasicParsing).StatusCode
```

> 若启动失败（端口一直不通），看日志：`Get-Content _boot.log -Tail 40`；
> 常见原因：MySQL/Redis 未启动、`8000` 端口被其他程序占用、Maven/JDK 未加入 PATH。

**改了前端源码后如何生效**

后端托管的是 `backend/src/main/resources/static/admin/` 下的**构建产物**，直接改 `frontend/src` 不会生效，
必须重新构建并拷贝后再重启后端：

```powershell
cd frontend
npm run build                                    # 构建产物输出到 frontend/admin
Copy-Item -Force 'admin\*' '..\backend\src\main\resources\static\admin\' -Recurse
Set-Location ..
.\restart_backend.cmd                            # 重启后端加载新产物
```

> `index.html` 已配置 `no-store` 不缓存、`js/css` 带内容哈希可长缓存，重启后浏览器强制刷新
> （`Ctrl+F5`）即可看到新版本。

#### 使用说明

**网页功能**
- 数据管理 / 用户分析 / 动漫属性分析：前端通过 ECharts 与后端统计接口展示动漫类型、语言、年份、评分与热度分布。
- 动漫数据探索（EDA）：后端把 `algorithm/eda/figures/` 下的 PNG 经 `/eda/**` 直接挂到页面，无需 Python 在线运行。
- 推荐 / 热度预测页：后端按需调用 `.venv` 中的 Python 引擎（`../algorithm/...` 相对定位），
  首次调用稍慢（模型加载），之后正常。

**命令行（教师演示 / 重跑实验）**

```bash
# 全部在 anime-system 根目录执行即可（脚本内部基于自身位置定位数据）

# 1. 重新构建数据集（5000 训练 / 5000 测试）
.venv\Scripts\python.exe scripts/data_prep/build_anime_dataset.py

# 2. 重新生成 EDA 图表
.venv\Scripts\python.exe algorithm/eda/eda_anime.py

# 3. 推荐引擎冒烟测试（输出 Top10 JSON）
.venv\Scripts\python.exe algorithm/recommendation/recommend_api.py --algo demographic --top 10

# 4. 热度预测引擎冒烟测试
.venv\Scripts\python.exe algorithm/prediction/predict_api.py --model rf --budget 12 --popularity 50000 --runtime 24 --language ja --status TV
```

**注意事项**
- 数据准备：原始数据在 `data/raw/anime-dataset-2023.csv`，运行
  `scripts/data_prep/build_anime_dataset.py` 即可生成 `data/train.csv`、`data/test.csv`
  与 `data/processed/` 下的标准化数据集。
- 数据库账号默认 `root / 123456`，如需修改请同步改 `backend/src/main/resources/application-dev.yml`。
- 只改 Java 后端：重启即可；改前端：需在 `frontend` 目录 `npm run build`，并把产物同步到
  `backend/src/main/resources/static/admin` 后重新打包。

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request
