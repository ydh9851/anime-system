# -*- coding: utf-8 -*-
"""
eda_anime.py
============
动漫数据分析（EDA）图表生成脚本（中文标签）。

数据源：
    data/processed/anime_train.csv     # 5000 条动漫标准化训练集

输出：
    algorithm/eda/figures/figXX_*.png  # 19 张中文 EDA 图表

运行：
    .venv\\Scripts\\python.exe algorithm/eda/eda_anime.py
"""

import os
import re

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

# ---------------------------------------------------------------------------
# 全局设置
# ---------------------------------------------------------------------------
BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(BASE))
DATA_CSV = os.path.join(ROOT, "data", "processed", "anime_train.csv")
FIG_DIR = os.path.join(BASE, "figures")
os.makedirs(FIG_DIR, exist_ok=True)

sns.set_style("whitegrid")
sns.set_context("notebook")
plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei", "Arial Unicode MS"]
plt.rcParams["axes.unicode_minus"] = False

PINK = "#ff6b9d"
PURPLE = "#9b59b6"
BLUE = "#4facfe"
GREEN = "#43e97b"
ORANGE = "#f6a623"

GENRE_MAP = {
    "Action": "动作", "Adventure": "冒险", "Comedy": "喜剧", "Drama": "剧情",
    "Fantasy": "奇幻", "Horror": "恐怖", "Mystery": "悬疑", "Romance": "恋爱",
    "Sci-Fi": "科幻", "Slice of Life": "日常", "Sports": "运动", "Supernatural": "超自然",
    "Thriller": "惊悚", "Music": "音乐", "Mecha": "机甲", "School": "校园",
    "Shounen": "少年", "Shoujo": "少女", "Seinen": "青年", "Josei": "女性",
    "Ecchi": "福利", "Game": "游戏", "Psychological": "心理", "Police": "警察",
    "Military": "军事", "Space": "太空", "Vampire": "吸血鬼", "Demons": "恶魔",
    "Historical": "历史", "Isekai": "异世界", "Martial Arts": "武术", "Magic": "魔法",
    "Super Power": "超能力", "Award Winning": "获奖作品", "Avant Garde": "先锋",
    "Parody": "恶搞", "Samurai": "武士", "Time Travel": "穿越", "Boys Love": "耽美",
    "Girls Love": "百合", "Mahou Shoujo": "魔法少女", "Idol": "偶像", "Iyashikei": "治愈",
    "Racing": "赛车", "Revenge": "复仇", "Suspense": "悬疑", "Workplace": "职场",
    "Detective": "侦探", "Mythology": "神话", "Survival": "生存", "Gourmet": "美食",
    "Kids": "儿童", "Educational": "教育", "Pets": "宠物", "Showbiz": "演艺圈",
}


def cn_genre(name):
    return GENRE_MAP.get(str(name).strip(), name)


def save(fig, name):
    path = os.path.join(FIG_DIR, name)
    fig.tight_layout()
    fig.savefig(path, dpi=120, bbox_inches="tight")
    plt.close(fig)
    print(f"[OK] {name}")


def short(text, n=26):
    text = str(text)
    return text if len(text) <= n else text[: n - 1] + "…"


# ---------------------------------------------------------------------------
# 读取数据
# ---------------------------------------------------------------------------
df = pd.read_csv(DATA_CSV)
df["score"] = pd.to_numeric(df["score"], errors="coerce")
df["members"] = pd.to_numeric(df["members"], errors="coerce")
df["favorites"] = pd.to_numeric(df["favorites"], errors="coerce")
df["popularity"] = pd.to_numeric(df["popularity"], errors="coerce")
df["episodes"] = pd.to_numeric(df["episodes"], errors="coerce")
df["duration_minutes"] = pd.to_numeric(df["duration_minutes"], errors="coerce")
df["year"] = pd.to_numeric(df["year"], errors="coerce")

# 展开类型标签
genre_series = df["genres"].dropna().str.split(",").explode().str.strip()
genre_series = genre_series[genre_series != ""]

# ===========================================================================
# 1. 动漫评分分布
# ===========================================================================
fig, ax = plt.subplots(figsize=(9, 5.5))
sns.histplot(df["score"].dropna(), bins=30, kde=True, color=PINK, ax=ax)
ax.set_title("动漫评分分布", fontsize=15, fontweight="bold")
ax.set_xlabel("评分")
ax.set_ylabel("动漫数量")
save(fig, "fig01_score_distribution.png")

# ===========================================================================
# 2. 热度（成员数）分布（对数）
# ===========================================================================
fig, ax = plt.subplots(figsize=(9, 5.5))
data = df.loc[df["members"] > 0, "members"]
sns.histplot(np.log10(data), bins=40, color=PURPLE, ax=ax)
ax.set_title("动漫热度分布（成员数 log10）", fontsize=15, fontweight="bold")
ax.set_xlabel("log10(成员数)")
ax.set_ylabel("动漫数量")
save(fig, "fig02_members_distribution.png")

# ===========================================================================
# 3. 动漫类型数量
# ===========================================================================
type_counts = df["type"].value_counts()
fig, ax = plt.subplots(figsize=(9, 5.5))
bars = ax.bar(type_counts.index, type_counts.values, color=BLUE)
ax.bar_label(bars, padding=3)
ax.set_title("动漫类型数量分布", fontsize=15, fontweight="bold")
ax.set_xlabel("类型")
ax.set_ylabel("数量")
save(fig, "fig03_type_distribution.png")

# ===========================================================================
# 4. 类型标签 Top15
# ===========================================================================
top_genres = genre_series.map(cn_genre).value_counts().head(15)
fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(top_genres.index[::-1], top_genres.values[::-1], color=GREEN)
ax.bar_label(bars, padding=3)
ax.set_title("动漫题材标签 Top15", fontsize=15, fontweight="bold")
ax.set_xlabel("动漫数量")
save(fig, "fig04_genre_top15.png")

# ===========================================================================
# 5. 各题材平均评分 Top15
# ===========================================================================
tmp = df.assign(genre=df["genres"].str.split(",")).explode("genre")
tmp["genre"] = tmp["genre"].str.strip().map(cn_genre)
genre_stat = (
    tmp[tmp["genre"] != ""]
    .groupby("genre")
    .agg(count=("score", "size"), avg_score=("score", "mean"))
    .query("count >= 100")
    .sort_values("avg_score", ascending=False)
    .head(15)
)
fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(genre_stat.index[::-1], genre_stat["avg_score"][::-1], color=ORANGE)
ax.bar_label(bars, fmt="%.2f", padding=3)
ax.set_xlim(0, 10)
ax.set_title("各题材平均评分 Top15（样本量≥100）", fontsize=15, fontweight="bold")
ax.set_xlabel("平均评分")
save(fig, "fig05_genre_avg_score.png")

# ===========================================================================
# 6. 各年份动漫数量
# ===========================================================================
year_counts = df[(df["year"] >= 1980) & (df["year"] <= 2023)]["year"].value_counts().sort_index()
fig, ax = plt.subplots(figsize=(12, 5.5))
ax.fill_between(year_counts.index, year_counts.values, color=PINK, alpha=0.25)
ax.plot(year_counts.index, year_counts.values, color=PINK, linewidth=2)
ax.set_title("各年份动漫数量趋势（1980-2023）", fontsize=15, fontweight="bold")
ax.set_xlabel("年份")
ax.set_ylabel("动漫数量")
save(fig, "fig06_year_trend.png")

# ===========================================================================
# 7. 评分 Top10 动漫
# ===========================================================================
top_score = df.nlargest(10, "score")[["english_name", "name", "score"]].copy()
top_score["label"] = top_score["english_name"].where(
    top_score["english_name"].notna() & (top_score["english_name"] != "UNKNOWN"),
    top_score["name"],
).apply(short)
fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(top_score["label"][::-1], top_score["score"][::-1], color=PINK)
ax.bar_label(bars, fmt="%.2f", padding=3)
ax.set_xlim(0, 10)
ax.set_title("评分 Top10 动漫", fontsize=15, fontweight="bold")
ax.set_xlabel("评分")
save(fig, "fig07_top_score.png")

# ===========================================================================
# 8. 热度 Top10 动漫
# ===========================================================================
top_members = df.nlargest(10, "members")[["english_name", "name", "members"]].copy()
top_members["label"] = top_members["english_name"].where(
    top_members["english_name"].notna() & (top_members["english_name"] != "UNKNOWN"),
    top_members["name"],
).apply(short)
fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(top_members["label"][::-1], top_members["members"][::-1], color=PURPLE)
ax.bar_label(bars, fmt="%.0f", padding=3)
ax.set_title("热度 Top10 动漫（成员数）", fontsize=15, fontweight="bold")
ax.set_xlabel("成员数")
save(fig, "fig08_top_members.png")

# ===========================================================================
# 9. 收藏 Top10 动漫
# ===========================================================================
top_fav = df.nlargest(10, "favorites")[["english_name", "name", "favorites"]].copy()
top_fav["label"] = top_fav["english_name"].where(
    top_fav["english_name"].notna() & (top_fav["english_name"] != "UNKNOWN"),
    top_fav["name"],
).apply(short)
fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(top_fav["label"][::-1], top_fav["favorites"][::-1], color=BLUE)
ax.bar_label(bars, fmt="%.0f", padding=3)
ax.set_title("收藏数 Top10 动漫", fontsize=15, fontweight="bold")
ax.set_xlabel("收藏数")
save(fig, "fig09_top_favorites.png")

# ===========================================================================
# 10. 原作来源分布
# ===========================================================================
source_counts = df["source"].replace({"UNKNOWN": "未知"}).value_counts().head(10)
fig, ax = plt.subplots(figsize=(9, 5.5))
bars = ax.bar(source_counts.index, source_counts.values, color=GREEN)
ax.bar_label(bars, padding=3)
plt.xticks(rotation=30, ha="right")
ax.set_title("动漫原作来源分布 Top10", fontsize=15, fontweight="bold")
ax.set_ylabel("动漫数量")
save(fig, "fig10_source_distribution.png")

# ===========================================================================
# 11. 制作公司 Top15
# ===========================================================================
studio_series = df["studios"].dropna().str.split(",").explode().str.strip()
studio_series = studio_series[(studio_series != "") & (studio_series != "UNKNOWN")]
top_studios = studio_series.value_counts().head(15)
fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(top_studios.index[::-1], top_studios.values[::-1], color=ORANGE)
ax.bar_label(bars, padding=3)
ax.set_title("制作公司作品数量 Top15", fontsize=15, fontweight="bold")
ax.set_xlabel("作品数量")
save(fig, "fig11_studios_top15.png")

# ===========================================================================
# 12. 分级分布
# ===========================================================================
rating_counts = df["rating"].replace({"UNKNOWN": "未知"}).value_counts().head(10)
fig, ax = plt.subplots(figsize=(10, 5.5))
bars = ax.bar(range(len(rating_counts)), rating_counts.values, color=PURPLE)
ax.set_xticks(range(len(rating_counts)))
ax.set_xticklabels([short(x, 22) for x in rating_counts.index], rotation=25, ha="right")
ax.bar_label(bars, padding=3)
ax.set_title("动漫分级分布", fontsize=15, fontweight="bold")
ax.set_ylabel("动漫数量")
save(fig, "fig12_rating_distribution.png")

# ===========================================================================
# 13. 集数分布
# ===========================================================================
fig, ax = plt.subplots(figsize=(9, 5.5))
sns.histplot(df.loc[df["episodes"] <= 100, "episodes"], bins=50, color=BLUE, ax=ax)
ax.set_title("动漫集数分布（≤100 集）", fontsize=15, fontweight="bold")
ax.set_xlabel("集数")
ax.set_ylabel("动漫数量")
save(fig, "fig13_episodes_distribution.png")

# ===========================================================================
# 14. 单集时长分布
# ===========================================================================
fig, ax = plt.subplots(figsize=(9, 5.5))
sns.histplot(df.loc[df["duration_minutes"] <= 180, "duration_minutes"].dropna(), bins=40, color=GREEN, ax=ax)
ax.set_title("单集时长分布（≤180 分钟）", fontsize=15, fontweight="bold")
ax.set_xlabel("单集时长（分钟）")
ax.set_ylabel("动漫数量")
save(fig, "fig14_duration_distribution.png")

# ===========================================================================
# 15. 评分 vs 热度散点
# ===========================================================================
sample = df[(df["members"] > 0)].sample(min(3000, len(df)), random_state=42)
fig, ax = plt.subplots(figsize=(9, 6))
ax.scatter(sample["score"], np.log10(sample["members"]), s=12, alpha=0.45, color=PINK)
ax.set_title("评分与热度关系（成员数 log10）", fontsize=15, fontweight="bold")
ax.set_xlabel("评分")
ax.set_ylabel("log10(成员数)")
save(fig, "fig15_score_vs_members.png")

# ===========================================================================
# 16. 各类型评分箱线图
# ===========================================================================
fig, ax = plt.subplots(figsize=(10, 6))
order = df["type"].value_counts().index.tolist()
sns.boxplot(data=df, x="type", y="score", order=order, palette="Set3", ax=ax)
ax.set_title("各类型动漫评分箱线图", fontsize=15, fontweight="bold")
ax.set_xlabel("类型")
ax.set_ylabel("评分")
save(fig, "fig16_type_score_box.png")

# ===========================================================================
# 17. 相关性热力图
# ===========================================================================
num_cols = ["score", "members", "favorites", "popularity", "episodes", "duration_minutes", "rank", "scored_by"]
corr = df[num_cols].corr()
fig, ax = plt.subplots(figsize=(9, 7))
sns.heatmap(corr, annot=True, fmt=".2f", cmap="RdPu", square=True, ax=ax,
            annot_kws={"fontsize": 9})
ax.set_title("数值特征相关性热力图", fontsize=15, fontweight="bold")
save(fig, "fig17_correlation_heatmap.png")

# ===========================================================================
# 18. 开播季度分布
# ===========================================================================
season_series = df["premiered"].dropna().astype(str)
season_series = season_series[season_series.str.contains("Spring|Summer|Fall|Winter", na=False)]
season = season_series.str.extract(r"(Spring|Summer|Fall|Winter)")[0].value_counts()
season = season.reindex(["Spring", "Summer", "Fall", "Winter"]).fillna(0)
season.index = ["春季", "夏季", "秋季", "冬季"]
fig, ax = plt.subplots(figsize=(8, 5.5))
bars = ax.bar(season.index, season.values, color=[PINK, GREEN, ORANGE, BLUE])
ax.bar_label(bars, padding=3)
ax.set_title("动漫开播季度分布", fontsize=15, fontweight="bold")
ax.set_ylabel("动漫数量")
save(fig, "fig18_premiered_season.png")

# ===========================================================================
# 19. 多语言动漫名称示例
# ===========================================================================
multi = df.dropna(subset=["name", "english_name", "japanese_name"]).head(12).copy()
multi["name"] = multi["name"].apply(lambda x: short(x, 22))
multi["english_name"] = multi["english_name"].apply(lambda x: short(x, 22))
multi["japanese_name"] = multi["japanese_name"].apply(lambda x: short(x, 16))
fig, ax = plt.subplots(figsize=(12, 6))
ax.axis("off")
table = ax.table(
    cellText=multi[["name", "english_name", "japanese_name"]].values,
    colLabels=["罗马音 / 日文名", "英文名", "其他语言名"],
    cellLoc="left",
    loc="center",
)
table.auto_set_font_size(False)
table.set_fontsize(9)
table.scale(1, 1.5)
for (row, col), cell in table.get_celld().items():
    if row == 0:
        cell.set_facecolor(PINK)
        cell.set_text_props(color="white", fontweight="bold")
    elif row % 2 == 0:
        cell.set_facecolor("#fff0f6")
ax.set_title("多语言动漫名称示例", fontsize=15, fontweight="bold", pad=16)
save(fig, "fig19_multilang_names.png")

print(f"\n[OK] 全部图表已生成到：{FIG_DIR}")
