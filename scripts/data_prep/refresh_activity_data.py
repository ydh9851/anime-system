# -*- coding: utf-8 -*-
"""
refresh_activity_data.py
======================
把「用户行为数据」刷新到最近 N 天，让首页看板的趋势图与统计指标有真实数据。

背景：
    业务库导入的数据时间戳全部落在导入当天（如 2026-09-24），
    而 DashboardController 的统计口径是「本月」，于是首页出现
    本月新增用户=1、本月播放量=0、最佳番剧为空、30 天趋势图只有孤零零一个点。
    本脚本不改任何业务代码，只把行为数据的时间轴平移到近期并补齐每日行为：

      1. t_video_play.last_play_time              -> 最近 N 天，越近播放次数越高
      2. t_user_video_operation.finally_active_time
      3. t_user.last_active_time
      4. t_user_event_log                         -> 按天重建，覆盖近 N 天
      5. 少量用户 create_time 移到近 N 天，让「本月新增用户」有数据

用法：
    .venv\\Scripts\\python.exe scripts/data_prep/refresh_activity_data.py
    .venv\\Scripts\\python.exe scripts/data_prep/refresh_activity_data.py --days 45

幂等：固定随机种子 + 事件日志按日期清理后重建，重复执行不会产生重复行。
"""

import argparse
import datetime as dt
import os
import random
import sys

import pymysql

# (行为描述, 权重)：权重越高代表该类行为越常见
ACTIONS = [
    ("登录了系统", 30),
    ("浏览了动漫详情页", 28),
    ("给动漫评分", 18),
    ("追番了", 12),
    ("搜索了动漫关键词", 8),
    ("导出了数据报表", 4),
]


def weighted_time(days, rnd):
    """最近 days 天内的随机时间，越靠近今天权重越高（帕累托衰减）"""
    offset = min(days - 1, max(0, int(rnd.paretovariate(1.6)) - 1))
    when = dt.datetime.now() - dt.timedelta(days=offset)
    return when.replace(
        hour=rnd.randint(8, 23),
        minute=rnd.randint(0, 59),
        second=rnd.randint(0, 59),
        microsecond=0)


def main():
    parser = argparse.ArgumentParser(prog="refresh_activity_data.py")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--db", default="anime_mangage_db")
    parser.add_argument("--days", type=int, default=30, help="行为数据回溯天数")
    parser.add_argument("--new-users", type=int, default=36, help="改为近期注册的用户数")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    rnd = random.Random(args.seed)
    conn = pymysql.connect(host=args.host, user=args.user, password=args.password,
                           database=args.db, charset="utf8mb4", autocommit=False)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT video_id, user_id FROM t_video_play")
            plays = cur.fetchall()
            cur.execute("SELECT id FROM t_user WHERE deleted = 0")
            users = [r[0] for r in cur.fetchall()]
            # t_user_video_operation 是 (id, video_id) 联合主键，id 即 user_id
            cur.execute("SELECT id, video_id FROM t_user_video_operation")
            operations = cur.fetchall()

        if not users:
            print("t_user 为空，请先导入用户数据")
            return 1

        # 1) 播放记录：时间轴平移 + 播放次数按时间衰减重算
        play_updates = []
        for video_id, user_id in plays:
            play_updates.append((weighted_time(args.days, rnd),
                                 max(1, int(rnd.paretovariate(2.0)) + 1),
                                 video_id, user_id))
        with conn.cursor() as cur:
            cur.executemany(
                "UPDATE t_video_play SET last_play_time = %s, play_times = %s "
                "WHERE video_id = %s AND user_id = %s",
                play_updates)

        # 2) 收藏 / 评分的最近活跃时间
        op_updates = [(weighted_time(args.days, rnd), uid, vid) for uid, vid in operations]
        with conn.cursor() as cur:
            cur.executemany(
                "UPDATE t_user_video_operation SET finally_active_time = %s " +
                "WHERE id = %s AND video_id = %s",
                op_updates)

        # 3) 用户最后活跃时间
        active_users = rnd.sample(users, min(len(users), max(60, len(users) // 6)))
        with conn.cursor() as cur:
            cur.executemany(
                "UPDATE t_user SET last_active_time = %s WHERE id = %s",
                [(weighted_time(args.days, rnd), uid) for uid in active_users])

        # 4) 行为日志：清掉窗口外的旧日志，再按天重建，保证近 N 天每天都有数据
        today = dt.date.today()
        boundary = dt.datetime.combine(today - dt.timedelta(days=args.days), dt.time.min)
        with conn.cursor() as cur:
            cur.execute("SELECT id, user_name FROM t_user WHERE deleted = 0")
            name_map = {r[0]: r[1] for r in cur.fetchall()}
            cur.execute("DELETE FROM t_user_event_log WHERE create_time < %s", (boundary,))

            actions, weights = zip(*ACTIONS)
            log_rows = []
            for day in range(args.days + 1):
                date = today - dt.timedelta(days=day)
                # 越近的日期行为越多，周末再乘 1.35
                volume = max(8, int(46 * (1.0 - day / (args.days + 12.0)) * rnd.uniform(0.6, 1.4)))
                if date.weekday() >= 5:
                    volume = int(volume * 1.35)
                for _ in range(volume):
                    uid = rnd.choice(users)
                    action = rnd.choices(actions, weights=weights)[0]
                    when = dt.datetime.combine(date, dt.time.min) + dt.timedelta(
                        hours=rnd.randint(7, 23), minutes=rnd.randint(0, 59),
                        seconds=rnd.randint(0, 59))
                    uname = name_map.get(uid, "user%s" % uid)
                    log_rows.append((uid, uname, "%s %s" % (uname, action), when))
            cur.executemany(
                "INSERT INTO t_user_event_log (user_id, user_name, content, create_time) "
                "VALUES (%s, %s, %s, %s)",
                log_rows)

        # 5) 少量用户改为近 N 天注册，让「本月新增用户」有数据
        recent = rnd.sample(users, min(args.new_users, len(users)))
        with conn.cursor() as cur:
            cur.executemany(
                "UPDATE t_user SET create_time = %s WHERE id = %s",
                [(weighted_time(args.days, rnd), uid) for uid in recent])

        conn.commit()
        print("行为数据已刷新至最近 %d 天：" % args.days)
        print("  t_video_play%d 条时间轴已平移" % len(play_updates))
        print("  t_user_video_operation    %d 条最近活跃时间已刷新" % len(op_updates))
        print("  t_user                    %d 个用户最后活跃时间已刷新" % len(active_users))
        print("  t_user_event_log          重建 %d 条（覆盖近 %d 天）" % (len(log_rows), args.days + 1))
        print("  t_user                    %d 个用户注册时间改为近 %d 天" % (len(recent), args.days))
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    for _s in (sys.stdout, sys.stderr):
        try:
            _s.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass
    sys.exit(main())
