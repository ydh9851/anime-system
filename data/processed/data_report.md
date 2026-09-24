# 动漫数据集清洗与抽样报告

## 1. 数据来源
- 源文件：`data/raw/anime-dataset-2023.csv`
- 原始规模：24905 行 × 24 列
- 有效样本（有评分且成员数 > 0）：15691 条
- 去重前：15691 条，去重后：15691 条

## 2. 初步分析发现
- `Score`、`Episodes`、`Rank`、`Scored By` 等字段在原始数据中是字符串，含大量 `UNKNOWN`，需转数值。
- `Score` 缺失量：9213 条（约 37%），评分缺失的动漫无法参与评分类分析，已剔除。
- `Members` 有少量为 0，已剔除。
- `Aired` 格式不统一（如 `Apr 3, 1998 to Apr 24, 1999`、`2001`），已解析出开播日期与年份。
- `Duration` 有 `24 min per ep`、`1 hr 55 min` 等格式，已统一解析为分钟。

## 3. 清洗与字段映射
| 原始字段 | 处理方式 | 新字段 | 说明 |
| --- | --- | --- | --- |
| Score | 转数值，剔除缺失 | score | 动漫评分（0-10） |
| Members | 转数值，剔除 0 | members / revenue | 成员数，作为热度指标（预测目标） |
| Favorites | 转数值 | favorites / popularity | 收藏数，作为预测特征中的热度指标 |
| Episodes | 转数值，缺失填 1 | episodes / budget | 集数 |
| Duration | 解析为分钟 | duration_minutes / runtime | 单集时长 |
| Type | 类别 | type / status | TV / Movie / OVA / ONA / Special / Music |
| Genres | 逗号分隔 | genres / primary_genre | 动漫类型标签 |
| Aired | 解析起始日期 | start_date / year / release_date | 开播日期 |

## 4. 抽样结果
- 训练集：`data/processed/anime_train.csv`，5000 条
- 测试集：`data/processed/anime_test.csv`，5000 条
- 预测训练集：`data/train.csv`，5000 条
- 预测测试集：`data/test.csv`，5000 条
- 抽样方式：按 `type` 分层随机抽样，`random_state=42`，保证各类动漫都有覆盖。

## 5. 训练集分布概览
- 评分：均值 6.37，中位数 6.36，最高 9.07
- 成员数：均值 60444，中位数 4858，最高 2882333
- 动漫类型分布：{'TV': 1429, 'OVA': 1046, 'Movie': 788, 'Special': 650, 'ONA': 614, 'Music': 473}
- 主要来源 Top10：{'Original': 1455, 'Manga': 1271, 'Unknown': 593, 'Visual novel': 354, 'Game': 296, 'Light novel': 275, 'Novel': 195, 'Other': 159, '4-koma manga': 100, 'Music': 90}

## 6. 字段说明（标准化后）
- `score`：动漫评分
- `genres` / `primary_genre`：类型标签 / 主类型
- `type`：TV / Movie / OVA / ONA / Special / Music
- `episodes`：集数
- `duration_minutes`：单集时长（分钟）
- `members`：成员数（热度）
- `popularity`：MAL 人气排名（数值越小越热门）
- `favorites`：收藏数
- `studios` / `primary_studio`：制作公司
- `source`：原作来源（Manga / Original / Light novel 等）
- `name` / `english_name` / `japanese_name`：多语言名称