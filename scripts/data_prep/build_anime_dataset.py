# -*- coding: utf-8 -*-
"""
build_anime_dataset.py
======================
动漫数据集构建脚本（数据清洗 → 随机抽样 → 生成训练/测试集）。

数据源：
    data/raw/anime-dataset-2023.csv           # 2.4 万+ 部动漫元数据

输出：
    data/processed/anime_train.csv            # 动漫标准化训练集（5000 条，含完整属性）
    data/processed/anime_test.csv             # 动漫标准化测试集（5000 条）
    data/train.csv                            # 热度预测训练集（5000 条，字段兼容算法）
    data/test.csv                             # 热度预测测试集（5000 条）
    data/processed/data_report.md             # 数据初析报告（缺失值/分布/清洗决策）

字段映射说明（把电影模板字段映射为动漫主题语义）：
    budget   -> 集数（episodes）
    revenue  -> 热度（members，成员数）
    runtime  -> 单集时长（分钟）
    status   -> 动漫类型（TV / Movie / OVA / ONA / Special / Music）
    popularity -> 人气值
"""

import os
import re
from datetime import datetime

import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split

# ---------------------------------------------------------------------------
# 路径
# ---------------------------------------------------------------------------
BASE = os.path.dirname(os.path.abspath(__file__))          # scripts/data_prep
ROOT = os.path.dirname(os.path.dirname(BASE))               # anime-system
RAW_CSV = os.path.join(ROOT, "data", "raw", "anime-dataset-2023.csv")
PROCESSED_DIR = os.path.join(ROOT, "data", "processed")
REPORT_PATH = os.path.join(PROCESSED_DIR, "data_report.md")

os.makedirs(PROCESSED_DIR, exist_ok=True)

# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------
MONTH_MAP = {
    "jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
    "jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12,
}


def parse_start_date(aired: str):
    """从 Aired 字段解析开播日期，返回 (ISO日期字符串, 年份)。解析失败返回 (None, None)。"""
    if not isinstance(aired, str):
        return None, None
    text = aired.strip()
    if not text or text.lower() in ("unknown", "not available"):
        return None, None

    # 只取起始日期：'Apr 3, 1998 to Apr 24, 1999' -> 'Apr 3, 1998'
    start = text.split(" to ")[0].strip()

    # 尝试多种格式
    for fmt in ("%b %d, %Y", "%b %Y", "%Y"):
        try:
            dt = datetime.strptime(start, fmt)
            return dt.strftime("%Y-%m-%d"), dt.year
        except ValueError:
            continue

    # 兜底：直接找 4 位年份
    m = re.search(r"(19|20)\d{2}", start)
    if m:
        year = int(m.group(0))
        return f"{year}-01-01", year
    return None, None


def parse_duration_minutes(duration: str):
    """把 '24 min per ep' / '1 hr 55 min' / '30 sec' 解析为分钟数。"""
    if not isinstance(duration, str):
        return np.nan
    text = duration.lower()
    if "unknown" in text:
        return np.nan
    minutes = 0.0
    found = False
    hr = re.search(r"(\d+(?:\.\d+)?)\s*hr", text)
    if hr:
        minutes += float(hr.group(1)) * 60
        found = True
    mi = re.search(r"(\d+(?:\.\d+)?)\s*min", text)
    if mi:
        minutes += float(mi.group(1))
        found = True
    se = re.search(r"(\d+(?:\.\d+)?)\s*sec", text)
    if se:
        minutes += float(se.group(1)) / 60.0
        found = True
    return round(minutes, 2) if found else np.nan


def first_item(value: str) -> str:
    """取逗号分隔列表的第一项，用于主类型/主制作公司等。"""
    if not isinstance(value, str):
        return ""
    parts = [p.strip() for p in value.split(",") if p.strip() and p.strip() != "UNKNOWN"]
    return parts[0] if parts else ""


# ---------------------------------------------------------------------------
# 1. 读取与初步分析
# ---------------------------------------------------------------------------
df = pd.read_csv(RAW_CSV)
raw_shape = df.shape
missing_before = df.isna().sum()

# 统一去除字符串首尾空格
for col in df.columns:
    if df[col].dtype == object:
        df[col] = df[col].astype(str).str.strip()

# 数值列转换（原数据中大量字段为字符串，含 UNKNOWN）
for col in ["Score", "Episodes", "Rank", "Popularity", "Favorites", "Scored By", "Members"]:
    df[col] = pd.to_numeric(df[col], errors="coerce")

# 动漫类型：UNKNOWN 视为缺失
df["Type"] = df["Type"].replace({"UNKNOWN": np.nan})

# 清洗规则：必须有评分、有成员、有明确动漫类型
clean = df.dropna(subset=["Score", "Members", "Type"])
clean = clean[clean["Members"] > 0].copy()

# 按 anime_id 去重
before_dedup = len(clean)
clean = clean.drop_duplicates(subset=["anime_id"], keep="first")
after_dedup = len(clean)

# 解析开播日期与时长
clean[["start_date", "year"]] = clean["Aired"].apply(lambda x: pd.Series(parse_start_date(x)))
clean["duration_minutes"] = clean["Duration"].apply(parse_duration_minutes)

# 主类型 / 主制作公司
clean["primary_genre"] = clean["Genres"].apply(first_item)
clean["primary_studio"] = clean["Studios"].apply(first_item)

# 字段重命名：统一的动漫语义字段
clean = clean.rename(columns={
    "anime_id": "anime_id",
    "Name": "name",
    "English name": "english_name",
    "Other name": "japanese_name",
    "Score": "score",
    "Genres": "genres",
    "Type": "type",
    "Episodes": "episodes",
    "Aired": "aired",
    "Premiered": "premiered",
    "Status": "status",
    "Producers": "producers",
    "Licensors": "licensors",
    "Studios": "studios",
    "Source": "source",
    "Duration": "duration",
    "Rating": "rating",
    "Rank": "rank",
    "Popularity": "popularity",
    "Favorites": "favorites",
    "Scored By": "scored_by",
    "Members": "members",
})

# 缺失值填补
clean["episodes"] = clean["episodes"].fillna(1).clip(lower=1).astype(int)
clean["duration_minutes"] = clean["duration_minutes"].fillna(clean["duration_minutes"].median())
clean["rank"] = clean["rank"].fillna(clean["rank"].max() + 1)
clean["favorites"] = clean["favorites"].fillna(0).astype(int)

# 需要保留的列
RICH_COLS = [
    "anime_id", "name", "english_name", "japanese_name", "score", "genres",
    "primary_genre", "type", "episodes", "aired", "start_date", "year",
    "premiered", "status", "producers", "licensors", "studios", "source",
    "duration", "duration_minutes", "rating", "rank", "popularity",
    "favorites", "scored_by", "members",
]
rich = clean[RICH_COLS].copy()

# ---------------------------------------------------------------------------
# 2. 分层随机抽样：5000 训练 + 5000 测试
# ---------------------------------------------------------------------------
# 先按 Type 分层抽 10000 条，再均分为 train/test，保证各类型动漫均有覆盖
strat_key = rich["type"].fillna("Other")
sampled, _ = train_test_split(
    rich, train_size=10000, random_state=42, stratify=strat_key
)
train_df, test_df = train_test_split(
    sampled, test_size=5000, random_state=42, stratify=sampled["type"].fillna("Other")
)

# ---------------------------------------------------------------------------
# 3. 生成算法兼容的预测数据集（data/train.csv / data/test.csv）
# ---------------------------------------------------------------------------
def to_prediction_format(d: pd.DataFrame) -> pd.DataFrame:
    """把动漫标准化数据映射为热度预测算法需要的字段。"""
    out = pd.DataFrame()
    out["id"] = d["anime_id"].astype(int)
    out["title"] = np.where(
        (d["english_name"].notna()) & (d["english_name"] != "UNKNOWN"),
        d["english_name"],
        d["name"],
    )
    out["budget"] = d["episodes"].astype(int)          # 集数
    out["revenue"] = d["members"].astype(int)          # 成员数热度
    out["runtime"] = d["duration_minutes"].round(1)    # 单集时长（分钟）
    out["popularity"] = d["favorites"].astype(float)   # 收藏数（热度）
    out["release_date"] = d["start_date"].fillna(
        d["year"].apply(lambda y: f"{int(y)}-01-01" if pd.notna(y) else None)
    )
    out["original_language"] = "ja"                    # 动漫主体语言
    out["status"] = d["type"].fillna("TV")             # 动漫类型
    out["genres"] = d["genres"]
    return out


pred_train = to_prediction_format(train_df)
pred_test = to_prediction_format(test_df)

# 写出
train_df.to_csv(os.path.join(PROCESSED_DIR, "anime_train.csv"), index=False, encoding="utf-8-sig")
test_df.to_csv(os.path.join(PROCESSED_DIR, "anime_test.csv"), index=False, encoding="utf-8-sig")
pred_train.to_csv(os.path.join(ROOT, "data", "train.csv"), index=False, encoding="utf-8-sig")
pred_test.to_csv(os.path.join(ROOT, "data", "test.csv"), index=False, encoding="utf-8-sig")

# ---------------------------------------------------------------------------
# 4. 数据初析报告
# ---------------------------------------------------------------------------
type_dist = train_df["type"].value_counts().to_dict()
source_dist = train_df["source"].value_counts().head(10).to_dict()
score_desc = train_df["score"].describe().to_dict()
members_desc = train_df["members"].describe().to_dict()

report_lines = [
    "# 动漫数据集清洗与抽样报告",
    "",
    "## 1. 数据来源",
    f"- 源文件：`data/raw/anime-dataset-2023.csv`",
    f"- 原始规模：{raw_shape[0]} 行 × {raw_shape[1]} 列",
    f"- 有效样本（有评分且成员数 > 0）：{len(clean)} 条",
    f"- 去重前：{before_dedup} 条，去重后：{after_dedup} 条",
    "",
    "## 2. 初步分析发现",
    "- `Score`、`Episodes`、`Rank`、`Scored By` 等字段在原始数据中是字符串，含大量 `UNKNOWN`，需转数值。",
    f"- `Score` 缺失量：{(pd.to_numeric(df['Score'], errors='coerce').isna()).sum()} 条（约 37%），评分缺失的动漫无法参与评分类分析，已剔除。",
    "- `Members` 有少量为 0，已剔除。",
    "- `Aired` 格式不统一（如 `Apr 3, 1998 to Apr 24, 1999`、`2001`），已解析出开播日期与年份。",
    "- `Duration` 有 `24 min per ep`、`1 hr 55 min` 等格式，已统一解析为分钟。",
    "",
    "## 3. 清洗与字段映射",
    "| 原始字段 | 处理方式 | 新字段 | 说明 |",
    "| --- | --- | --- | --- |",
    "| Score | 转数值，剔除缺失 | score | 动漫评分（0-10） |",
    "| Members | 转数值，剔除 0 | members / revenue | 成员数，作为热度指标（预测目标） |",
    "| Favorites | 转数值 | favorites / popularity | 收藏数，作为预测特征中的热度指标 |",
    "| Episodes | 转数值，缺失填 1 | episodes / budget | 集数 |",
    "| Duration | 解析为分钟 | duration_minutes / runtime | 单集时长 |",
    "| Type | 类别 | type / status | TV / Movie / OVA / ONA / Special / Music |",
    "| Genres | 逗号分隔 | genres / primary_genre | 动漫类型标签 |",
    "| Aired | 解析起始日期 | start_date / year / release_date | 开播日期 |",
    "",
    "## 4. 抽样结果",
    f"- 训练集：`data/processed/anime_train.csv`，{len(train_df)} 条",
    f"- 测试集：`data/processed/anime_test.csv`，{len(test_df)} 条",
    f"- 预测训练集：`data/train.csv`，{len(pred_train)} 条",
    f"- 预测测试集：`data/test.csv`，{len(pred_test)} 条",
    "- 抽样方式：按 `type` 分层随机抽样，`random_state=42`，保证各类动漫都有覆盖。",
    "",
    "## 5. 训练集分布概览",
    f"- 评分：均值 {score_desc['mean']:.2f}，中位数 {score_desc['50%']:.2f}，最高 {score_desc['max']:.2f}",
    f"- 成员数：均值 {members_desc['mean']:.0f}，中位数 {members_desc['50%']:.0f}，最高 {members_desc['max']:.0f}",
    f"- 动漫类型分布：{type_dist}",
    f"- 主要来源 Top10：{source_dist}",
    "",
    "## 6. 字段说明（标准化后）",
    "- `score`：动漫评分",
    "- `genres` / `primary_genre`：类型标签 / 主类型",
    "- `type`：TV / Movie / OVA / ONA / Special / Music",
    "- `episodes`：集数",
    "- `duration_minutes`：单集时长（分钟）",
    "- `members`：成员数（热度）",
    "- `popularity`：MAL 人气排名（数值越小越热门）",
    "- `favorites`：收藏数",
    "- `studios` / `primary_studio`：制作公司",
    "- `source`：原作来源（Manga / Original / Light novel 等）",
    "- `name` / `english_name` / `japanese_name`：多语言名称",
]

with open(REPORT_PATH, "w", encoding="utf-8") as f:
    f.write("\n".join(report_lines))

print("[OK] 数据集构建完成")
print(f"  data/processed/anime_train.csv : {len(train_df)} 条")
print(f"  data/processed/anime_test.csv  : {len(test_df)} 条")
print(f"  data/train.csv                 : {len(pred_train)} 条")
print(f"  data/test.csv                  : {len(pred_test)} 条")
print(f"  data/processed/data_report.md  : 数据分析报告")
