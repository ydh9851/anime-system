# -*- coding: utf-8 -*-
"""
build_full_dataset.py
=====================
用 anime-dataset-2023.csv 全量（24905 部）重建 `t_video_info`，
并同步重建题材标签（t_tag / t_video_tag）。

新增能力：
    1. 全量扩充（由抽样 5000 → 全量 24905）
    2. 国家/地区派生（country：JP/CN/US/KR，依据真实制片方白名单，默认日本）
    3. 海报直链（poster_path <- Image URL）
    4. 制作公司（studios）、来源（source）、首播季度（premiered）、
       动画类型（video_type）、简介（overview <- Synopsis）

用法：
    .venv\\Scripts\\python.exe scripts/data_prep/build_full_dataset.py --password 123456
"""

import argparse
import os
import re
from datetime import datetime

import numpy as np
import pandas as pd
import pymysql

BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(BASE))
RAW_CSV = os.path.join(ROOT, "data", "raw", "anime-dataset-2023.csv")

# ---------------------------------------------------------------------------
# 国家派生白名单（仅收录可确认的非日本制片/出品方，避免把日式外包误判）
# 命中任一关键词 -> 对应国家；否则默认日本（MAL 数据集主体为日漫）
# ---------------------------------------------------------------------------
COUNTRY_RULES = {
    "US": [
        "adult swim", "cartoon network", "warner bros", "dreamworks",
        "rooster teeth", "titmouse", "marvel", "dc comics", "frederator",
        "nickelodeon", "universal", "pixar", "sony pictures animation",
    ],
    "CN": [
        "shanghai animation", "beijing huihuang", "fantawild",
        "bilibili", "tencent", "cctv",
    ],
    "KR": [
        "studio mir", "samg", "red dog", "studio pivot", "studio n", "dr. movie",
    ],
}
LANG_MAP = {"JP": "ja", "US": "en", "CN": "zh", "KR": "ko", "OTHER": "en"}


def derive_country(studios):
    s = str(studios) if pd.notna(studios) else ""
    for country, keys in COUNTRY_RULES.items():
        if any(k in s.lower() for k in keys):
            return country
    return "JP"


def parse_start_date(aired):
    if not isinstance(aired, str):
        return None
    text = aired.strip()
    if not text or text.lower() in ("unknown", "not available"):
        return None
    start = text.split(" to ")[0].strip()
    for fmt in ("%b %d, %Y", "%b %Y", "%Y"):
        try:
            return datetime.strptime(start, fmt).strftime("%Y-%m-%d")
        except ValueError:
            continue
    m = re.search(r"(19|20)\d{2}", start)
    if m:
        return f"{m.group(0)}-01-01"
    return None


def parse_duration_minutes(duration):
    if not isinstance(duration, str):
        return None
    text = duration.lower()
    if "unknown" in text:
        return None
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
    return int(round(minutes)) if found else None


def to_int(v):
    if v is None:
        return None
    s = str(v).replace(",", "").strip()
    if s.lower() in ("", "nan", "none", "unknown", "<na>"):
        return None
    try:
        f = float(s)
        if np.isnan(f) or np.isinf(f):
            return None
        return int(f)
    except (ValueError, TypeError):
        return None


def to_float(v):
    if v is None:
        return None
    s = str(v).strip()
    if s.lower() in ("", "nan", "none", "unknown", "<na>"):
        return None
    try:
        f = float(s)
        return None if np.isnan(f) or np.isinf(f) else f
    except (ValueError, TypeError):
        return None


def sanitize_row(row):
    """兜底：任何残余 NaN/NA 统一转 None，规避 pymysql 拒绝 float nan"""
    out = []
    for v in row:
        try:
            if v is None:
                out.append(None)
            elif isinstance(v, float) and np.isnan(v):
                out.append(None)
            elif pd.isna(v):
                out.append(None)
            else:
                out.append(v)
        except (TypeError, ValueError):
            out.append(v)
    return tuple(out)


def clean(v):
    if v is None:
        return None
    try:
        if pd.isna(v):
            return None
    except (TypeError, ValueError):
        pass
    s = str(v).strip()
    return None if s.lower() in ("nan", "none", "unknown", "<na>") else s


def ensure_column(conn, table, col, ddl):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s AND COLUMN_NAME = %s
        """, (table, col))
        if cur.fetchone()[0] == 0:
            cur.execute(f"ALTER TABLE `{table}` ADD COLUMN {ddl}")
            print(f"  [ADD COLUMN] {table}.{col}")


def drop_fks_to(conn, parent):
    """摘除所有引用 parent 表的外键，返回约束描述以便重建"""
    with conn.cursor() as cur:
        cur.execute("""
            SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE REFERENCED_TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = %s
        """, (parent,))
        fks = [(t, c, col, ref_col) for t, c, col, ref_col in cur.fetchall()]
        for tbl, name, _, _ in fks:
            cur.execute(f"ALTER TABLE `{tbl}` DROP FOREIGN KEY `{name}`")
            print(f"  [DROP FK] {tbl}.{name} → {parent}")
        return fks


def restore_fks(conn, parent, fks):
    with conn.cursor() as cur:
        for tbl, name, col, ref_col in fks:
            cur.execute(
                f"ALTER TABLE `{tbl}` ADD CONSTRAINT `{name}` FOREIGN KEY (`{col}`) "
                f"REFERENCES `{parent}` (`{ref_col}`)")
            print(f"  [ADD FK] {tbl}.{name}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="")
    ap.add_argument("--db", default="anime_mangage_db")
    args = ap.parse_args()

    print("[1/4] 读取原始数据集 ...")
    df = pd.read_csv(RAW_CSV)
    print(f"      原始记录数: {len(df)}")

    # 字段映射（label 列 -> 语义）
    df["country"] = df["Studios"].apply(derive_country)
    df["language"] = df["country"].map(LANG_MAP)
    df["release_date"] = df["Aired"].apply(parse_start_date)
    df["runtime"] = df["Duration"].apply(parse_duration_minutes)
    df["budget"] = df["Episodes"].apply(to_int)          # 集数
    df["revenue"] = df["Members"].apply(to_int)          # 成员数（热度）
    df["popularity"] = df["Favorites"].apply(to_float)   # 收藏数
    df["vote_average"] = df["Score"].apply(to_float)
    df["vote_count"] = df["Scored By"].apply(to_int)

    conn = pymysql.connect(host=args.host, user=args.user, password=args.password,
                           database=args.db, charset="utf8mb4", autocommit=False)

    print("[2/4] 扩展表结构（studios/country/source/premiered/video_type）...")
    ensure_column(conn, "t_video_info", "studios", "`studios` varchar(500) NULL COMMENT '制作公司' AFTER `original_language`")
    ensure_column(conn, "t_video_info", "country", "`country` varchar(20) NULL DEFAULT 'JP' COMMENT '国家/地区' AFTER `studios`")
    ensure_column(conn, "t_video_info", "source", "`source` varchar(30) NULL COMMENT '原作来源' AFTER `country`")
    ensure_column(conn, "t_video_info", "premiered", "`premiered` varchar(30) NULL COMMENT '首播季度' AFTER `source`")
    ensure_column(conn, "t_video_info", "video_type", "`video_type` varchar(20) NULL COMMENT '动画类型' AFTER `premiered`")

    print("[3/4] 重建 t_video_info / t_tag / t_video_tag（全量导入）...")
    fks = drop_fks_to(conn, "t_video_info")
    fks_tag = drop_fks_to(conn, "t_tag")
    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE t_video_info")
        cur.execute("TRUNCATE TABLE t_video_tag")
        cur.execute("TRUNCATE TABLE t_tag")

    insert_sql = """
        INSERT INTO t_video_info
          (video_id, video_name, original_title, overview, tagline,
           budget, revenue, popularity, vote_average, vote_count, runtime,
           release_date, original_language, poster_path, heat_score,
           video_category, create_time, creator_id, last_modify_time, video_url,
           studios, country, source, premiered, video_type)
        VALUES
          (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,0,3,CURDATE(),2,NOW(),%s,%s,%s,%s,%s,%s)
    """

    rows, BATCH = [], 2000
    for _, r in df.iterrows():
        name = clean(r["Name"])
        eng = clean(r.get("English name"))
        tagline = clean(r.get("Other name"))
        rows.append((
            int(r["anime_id"]), name, eng or name,
            clean(r.get("Synopsis")), tagline,
            r["budget"], r["revenue"], r["popularity"], r["vote_average"], r["vote_count"], r["runtime"],
            r["release_date"], r["language"], clean(r.get("Image URL")),
            clean(r.get("Image URL")),
            clean(r.get("Studios")), r["country"], clean(r.get("Source")),
            clean(r.get("Premiered")), clean(r.get("Type")),
        ))
        if len(rows) >= BATCH:
            with conn.cursor() as cur:
                cur.executemany(insert_sql, [sanitize_row(r) for r in rows])
            rows = []
    if rows:
        with conn.cursor() as cur:
            cur.executemany(insert_sql, [sanitize_row(r) for r in rows])

    # 题材标签重建（Genres 为逗号分隔字符串）
    tag_id = {}

    def tag_id_for(name):
        name = str(name).strip()
        if not name:
            return None
        if name not in tag_id:
            with conn.cursor() as cur:
                cur.execute("INSERT INTO t_tag (tag_name) VALUES (%s)", (name,))
                tag_id[name] = cur.lastrowid
        return tag_id[name]

    vt_rows, BATCH2 = [], 3000
    for _, r in df.iterrows():
        genres = str(r.get("Genres")) if pd.notna(r.get("Genres")) else ""
        for g in genres.split(","):
            tid = tag_id_for(g)
            if tid:
                vt_rows.append((int(r["anime_id"]), tid, 1))
        if len(vt_rows) >= BATCH2:
            with conn.cursor() as cur:
                cur.executemany(
                    "INSERT IGNORE INTO t_video_tag (video_id, tag_id, video_property_flag) VALUES (%s,%s,%s)",
                    vt_rows)
            vt_rows = []
    if vt_rows:
        with conn.cursor() as cur:
            cur.executemany(
                "INSERT IGNORE INTO t_video_tag (video_id, tag_id, video_property_flag) VALUES (%s,%s,%s)",
                vt_rows)

    conn.commit()

    # 重建此前摘除的外键
    restore_fks(conn, "t_video_info", fks)
    restore_fks(conn, "t_tag", fks_tag)
    conn.commit()

    print("[4/4] 校验 ...")
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM t_video_info")
        print(f"  t_video_info: {cur.fetchone()[0]}")
        cur.execute("SELECT COUNT(*) FROM t_tag")
        print(f"  t_tag: {cur.fetchone()[0]}")
        cur.execute("SELECT COUNT(*) FROM t_video_tag")
        print(f"  t_video_tag: {cur.fetchone()[0]}")
        cur.execute("SELECT country, COUNT(*) FROM t_video_info GROUP BY country ORDER BY COUNT(*) DESC")
        print("  国家分布:", dict(cur.fetchall()))
    conn.close()
    print("[OK] 全量数据集构建完成")


if __name__ == "__main__":
    main()