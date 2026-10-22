"""
检测 t_video_info.poster_path 中的失效封面链接。

MyAnimeList 的 CDN 会清理旧图片，数据集里的部分 poster_path 已返回 404，
导致前端列表出现破图。本脚本按批并发探测，把失效的链接写入
data/poster_dead.txt（每行一个 video_id），供人工复核或后续清理。

用法：
    .venv\\Scripts\\python.exe scripts\\data_prep\\check_posters.py --limit 3000
    .venv\\Scripts\\python.exe scripts\\data_prep\\check_posters.py --all
"""
import argparse
import concurrent.futures
import os
import sys

import pymysql

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def probe(url):
    """返回 True 表示图片可用"""
    try:
        import urllib.request
        req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status == 200
    except Exception:
        return False


def main():
    ap = argparse.ArgumentParser(description="检测失效封面链接")
    ap.add_argument("--password", default="123456", help="MySQL root 密码")
    ap.add_argument("--limit", type=int, default=500, help="本次最多检测多少条")
    ap.add_argument("--all", action="store_true", help="检测全部")
    ap.add_argument("--workers", type=int, default=16, help="并发数")
    ap.add_argument("--out", default=os.path.join(ROOT, "data", "poster_dead.txt"))
    args = ap.parse_args()

    conn = pymysql.connect(user="root", password=args.password, database="anime_mangage_db",
                           charset="utf8mb4")
    sql = ("SELECT video_id, poster_path FROM t_video_info "
           "WHERE poster_path IS NOT NULL AND poster_path <> %s")
    if not args.all:
        sql += " LIMIT %d" % args.limit
    with conn.cursor() as cur:
        cur.execute(sql, ("",))
        rows = cur.fetchall()
    conn.close()

    total = len(rows)
    if total == 0:
        print("没有需要检测的记录")
        return
    print("开始检测 %d 条封面..." % total)

    dead = []
    done = 0
    with concurrent.futures.ThreadPoolExecutor(args.workers) as ex:
        futures = {ex.submit(probe, url): vid for vid, url in rows}
        for fu in concurrent.futures.as_completed(futures):
            done += 1
            if not fu.result():
                dead.append(futures[fu])
            if done % 500 == 0:
                print("  已检测 %d/%d，失效 %d" % (done, total, len(dead)))

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        for vid in sorted(dead):
            f.write(str(vid) + "\n")

    print("检测完成：%d 条中有 %d 条失效（%.1f%%）" % (total, len(dead), len(dead) * 100.0 / total))
    print("失效 video_id 已写入: %s" % args.out)
    print("提示：可在 MySQL 中执行 UPDATE t_video_info SET poster_path = NULL WHERE video_id IN (...) 进行清理")


if __name__ == "__main__":
    main()

