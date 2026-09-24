# -*- coding: utf-8 -*-
"""
enrich_video_info.py
====================
用原始动漫数据集补全 MySQL `t_video_info` 中缺失的属性字段。

背景：
    历史导入的 t_video_info 中 release_date / runtime / budget 全部为空，
    导致「动漫属性分析」中的年份分布、时长分布、集数-热度散点为空图。

补全字段（按 video_id = anime_id 匹配）：
    release_date  <- Aired 解析出的开播日期
    runtime       <- Duration 解析出的单集时长（分钟）
    budget        <- Episodes（集数）
    vote_average  <- Score（评分）
    vote_count    <- Scored By（评分人数）

用法：
    .venv\\Scripts\\python.exe scripts/data_prep/enrich_video_info.py --password 123456
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="")
    ap.add_argument("--db", default="anime_mangage_db")
    args = ap.parse_args()

    df = pd.read_csv(RAW_CSV)
    for col in df.columns:
        if df[col].dtype == object:
            df[col] = df[col].astype(str).str.strip()

    df["score"] = df["Score"].apply(to_float)
    df["scored_by"] = df["Scored By"].apply(to_int)
    df["episodes"] = df["Episodes"].apply(to_int)
    df["release_date"] = df["Aired"].apply(parse_start_date)
    df["runtime"] = df["Duration"].apply(parse_duration_minutes)

    # 只保留能补全出有效开播日期的记录
    df = df[df["release_date"].notna()].copy()
    df["genres"] = df["Genres"]

    conn = pymysql.connect(host=args.host, user=args.user, password=args.password,
                           database=args.db, charset="utf8mb4", autocommit=False)
    cur = conn.cursor()

    sql = """
        UPDATE t_video_info
        SET release_date = %s,
            runtime = %s,
            budget = %s,
            vote_average = %s,
            vote_count = %s
        WHERE video_id = %s
    """

    def clean(v):
        if v is None:
            return None
        try:
            if pd.isna(v):
                return None
        except (TypeError, ValueError):
            pass
        return v

    rows = []
    for _, r in df.iterrows():
        rows.append((
            clean(r["release_date"]),
            clean(r["runtime"]),
            clean(r["episodes"]) if clean(r["episodes"]) else 1,
            clean(r["score"]),
            clean(r["scored_by"]),
            int(r["anime_id"]),
        ))

    cur.executemany(sql, rows)
    conn.commit()
    print(f"[OK] 已补全 {cur.rowcount if cur.rowcount > 0 else len(rows)} 条动漫属性记录")

    cur.execute("""
        SELECT COUNT(*) total,
               SUM(release_date IS NULL) null_release,
               SUM(runtime IS NULL OR runtime = 0) null_runtime,
               SUM(budget IS NULL OR budget = 0) null_budget,
               SUM(vote_average IS NULL OR vote_average = 0) null_vote
        FROM t_video_info
    """)
    print("[CHECK]", cur.fetchone())
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
