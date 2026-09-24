# -*- coding: utf-8 -*-
"""
predict_api.py
================
动漫热度预测命令行封装：把动漫模板的热度预测模型改造为动漫主题，
供 Java 后端通过 ProcessBuilder 调用（与 recommend_api.py 同一契约风格）。

原始特征：
    original_language / budget(集数) / popularity(收藏数) / runtime(单集时长) / status(类型)

特征工程（v2，见《03-算法设计说明书》3.1 节）：
    ① 对数变换：budget_log = ln(1+集数)、popularity_log = ln(1+收藏数)
       —— 破解 members/favorites 的幂律长尾分布（最大 288 万，均值 6 万）
    ② 交互特征：interact = budget_log × ln(1+runtime) —— 捕获"集数×时长"复合影响
    ③ 标准化：LR / KNN / SVR 前置 StandardScaler（Pipeline 封装），消除量纲差异
    ④ 标签变换：训练 y' = ln(1+热度)，预测后 expm1 还原到原始量纲
    ⑤ 缓存版本化：bundle 带 version，代码升级后自动失效重训，避免脏缓存

模型（6 种）与缓存：
    lr / knn / svm / dt / rf / lgbm  ->  model_cache/predict_<model>.joblib
    - 首次调用自动训练并缓存；之后直接加载缓存，秒级返回。

用法:
    # 单条预测（首次会自动训练）
    python predict_api.py --model rf --budget 24 --popularity 50000 --runtime 24 --language ja --status TV

    # 仅预训练指定模型（建议部署后先跑一次预热）
    python predict_api.py --train --model lgbm

成功输出（单行 JSON）:
    {"model": "rf", "prediction": 123456.0, "currency": "members",
     "trained_now": false, "cache_file": "...",
     "metrics": {"rmse": ..., "mae": ..., "r2": ...}}

失败输出（单行 JSON，退出码非 0）:
    {"model": "...", "error": "...", "trace": "..."}
"""

import argparse
import contextlib
import io
import json
import os
import sys
import traceback
import warnings

warnings.filterwarnings("ignore")

import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402


def _silent():
    """屏蔽第三方库在 import / fit 时打印到 stdout 的信息，保证输出只有一行 JSON。"""
    stack = contextlib.ExitStack()
    stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
    stack.enter_context(contextlib.redirect_stderr(io.StringIO()))
    return stack

BASE = os.path.dirname(os.path.abspath(__file__))
SYSTEM_ROOT = os.path.dirname(os.path.dirname(BASE))  # 系统工程根目录 anime-system/
TRAIN_CSV = os.path.join(SYSTEM_ROOT, "data", "train.csv")
CACHE_DIR = os.path.join(BASE, "model_cache")

# 数值列用中位数填充，类别列用众数填充
NUMERIC_COLS = ["budget", "popularity", "runtime"]
CAT_COLS = ["original_language", "status"]

# 缓存版本：特征工程或模型结构变更时递增（旧缓存自动失效重训）
FEATURE_VERSION = 2

# 模型实际使用的特征（顺序固定，predict 时保持一致）
FEATURE_NAMES = ["budget_log", "popularity_log", "runtime", "interact",
                 "original_language_code", "status_code"]


def _stdout_reconfigure():
    """Windows 管道下统一 UTF-8 输出，Java 端按 UTF-8 解码。"""
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass


def load_and_prepare():
    """读取 data/train.csv，完成清洗 + 特征工程，返回 (X, y_log, y_raw, meta)。"""
    df = pd.read_csv(TRAIN_CSV)
    df = df[CAT_COLS + NUMERIC_COLS + ["revenue"]].copy()

    for col in NUMERIC_COLS:
        df[col] = pd.to_numeric(df[col], errors="coerce")
        median = df[col].median()
        # 注意：不要用 df[col].fillna(..., inplace=True)（链式赋值可能不生效）
        df[col] = df[col].fillna(0.0 if pd.isna(median) else median)
    for col in CAT_COLS:
        df[col] = df[col].fillna(df[col].mode()[0]).astype(str)

    lang_codes = {v: i for i, v in enumerate(df["original_language"].unique())}
    status_codes = {v: i for i, v in enumerate(df["status"].unique())}
    df["original_language_code"] = df["original_language"].map(lang_codes)
    df["status_code"] = df["status"].map(status_codes)

    # ---- 特征工程 ① 对数变换（破解长尾） ----
    df["budget_log"] = np.log1p(df["budget"].clip(lower=0))
    df["popularity_log"] = np.log1p(df["popularity"].clip(lower=0))
    df["runtime"] = df["runtime"].clip(lower=0)
    # ---- 特征工程 ② 交互特征 ----
    df["interact"] = df["budget_log"] * np.log1p(df["runtime"])

    meta = {
        "lang_codes": lang_codes,
        "status_codes": status_codes,
        "lang_mode": str(df["original_language"].mode()[0]),
        "status_mode": str(df["status"].mode()[0]),
        "global_mean_revenue": float(df["revenue"].mean()),
    }
    X = df[FEATURE_NAMES]
    y_raw = df["revenue"].astype(float)          # 原始量纲（用于评估）
    y_log = np.log1p(y_raw.clip(lower=0))        # 对数量纲（用于训练）
    # 训练前最终兜底：杜绝 NaN 进入 sklearn 模型
    X = X.fillna(X.median()).fillna(0.0)
    return X, y_log, y_raw, meta


def build_model(name, X_train, y_train):
    """按名称构建并训练模型（仅训练集拟合）。

    LR / KNN / SVR 对特征量纲敏感，统一用 Pipeline 前置 StandardScaler。
    """
    from sklearn.pipeline import make_pipeline
    from sklearn.preprocessing import StandardScaler

    if name == "lgbm":
        import lightgbm as lgb
        params = {
            "objective": "regression",
            "metric": "rmse",
            "boosting_type": "gbdt",
            "learning_rate": 0.05,
            "num_leaves": 31,
            "feature_fraction": 0.8,
            "bagging_fraction": 0.8,
            "bagging_freq": 5,
            "verbose": -1,
        }
        return lgb.train(params, lgb.Dataset(X_train, label=y_train), num_boost_round=500)

    if name == "rf":
        from sklearn.ensemble import RandomForestRegressor
        model = RandomForestRegressor(n_estimators=200, random_state=42, n_jobs=-1)

    elif name == "lr":
        # 线性回归：标准化后最小二乘
        from sklearn.linear_model import LinearRegression
        model = make_pipeline(StandardScaler(), LinearRegression())

    elif name == "knn":
        # 对应 KNN.py：K 近邻回归（距离加权）
        from sklearn.neighbors import KNeighborsRegressor
        model = make_pipeline(StandardScaler(),
                              KNeighborsRegressor(n_neighbors=5, weights="distance", n_jobs=-1))

    elif name == "svm":
        # 对应 SVM.py：支持向量回归（RBF 核）
        from sklearn.svm import SVR
        model = make_pipeline(StandardScaler(),
                              SVR(kernel="rbf", C=10.0, gamma="scale", epsilon=0.1))

    elif name == "dt":
        # 对应 DecisionTree.py：决策树回归
        from sklearn.tree import DecisionTreeRegressor
        model = DecisionTreeRegressor(random_state=42)

    else:
        raise ValueError("unknown model: %s" % name)

    model.fit(X_train, y_train)
    return model


def calc_metrics(model, X_test, y_log_test, y_raw_test):
    """双空间评估：

    - R²：在**对数空间**计算。热度呈幂律长尾，log 变换后误差分布更稳健，
      各模型口径一致、可横向对比（避免 expm1 还原把低方差模型误差放大）。
    - RMSE / MAE：在**原始量纲（成员数）** 计算，保证业务可解释。
    """
    from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
    pred_log = model.predict(X_test)
    pred_raw = np.expm1(pred_log)
    r2_log = float(r2_score(y_log_test, pred_log))
    return {
        "rmse": float(np.sqrt(mean_squared_error(y_raw_test, pred_raw))),
        "mae": float(mean_absolute_error(y_raw_test, pred_raw)),
        "r2": r2_log,
        "r2_log": r2_log,
    }


def cache_path(name):
    return os.path.join(CACHE_DIR, "predict_%s.joblib" % name)


def train_and_cache(name):
    """训练 + 测试集评估 + 缓存到 model_cache（带版本号）。"""
    import joblib
    from sklearn.model_selection import train_test_split

    X, y_log, y_raw, meta = load_and_prepare()
    X_train, X_test, yl_train, yl_test, yr_train, yr_test = train_test_split(
        X, y_log, y_raw, test_size=0.2, random_state=42
    )
    model = build_model(name, X_train, yl_train)
    met = calc_metrics(model, X_test, yl_test, yr_test)

    os.makedirs(CACHE_DIR, exist_ok=True)
    bundle = {
        "model": model,
        "metrics": met,
        "feature_names": FEATURE_NAMES,
        "meta": meta,
        "version": FEATURE_VERSION,
        "target": "log1p",
    }
    joblib.dump(bundle, cache_path(name))
    return met


def load_or_train(name):
    """优先加载缓存（校验版本），不存在或版本不符则训练。返回 (bundle, trained_now)。"""
    import joblib
    path = cache_path(name)
    if os.path.exists(path):
        with _silent():
            bundle = joblib.load(path)
        # 版本不符（特征工程/模型结构已升级）-> 丢弃旧缓存重训
        if isinstance(bundle, dict) and bundle.get("version") == FEATURE_VERSION:
            return bundle, False
    with _silent():
        train_and_cache(name)
    with _silent():
        return joblib.load(path), True


def _encode(codes, value, mode_value):
    s = str(value)
    if s in codes:
        return codes[s]
    # 训练集中未出现过的取值：回退到训练集众数类别
    return codes[mode_value]


def build_input_row(budget, popularity, runtime, lang_code, status_code):
    """构造单条预测输入（与训练期特征工程完全一致）。"""
    budget = max(0.0, float(budget))
    popularity = max(0.0, float(popularity))
    runtime = max(0.0, float(runtime))
    budget_log = float(np.log1p(budget))
    popularity_log = float(np.log1p(popularity))
    return pd.DataFrame([{
        "budget_log": budget_log,
        "popularity_log": popularity_log,
        "runtime": runtime,
        "interact": budget_log * float(np.log1p(runtime)),
        "original_language_code": lang_code,
        "status_code": status_code,
    }])[FEATURE_NAMES]


def predict(name, budget, popularity, runtime, language, status):
    bundle, trained_now = load_or_train(name)
    model = bundle["model"]
    meta = bundle["meta"]

    lang_code = _encode(meta["lang_codes"], language, meta["lang_mode"])
    status_code = _encode(meta["status_codes"], status, meta["status_mode"])
    row = build_input_row(budget, popularity, runtime, lang_code, status_code)

    pred_log = float(model.predict(row)[0])
    pred = float(np.expm1(pred_log))  # 还原到原始量纲（成员数）
    return pred, trained_now, bundle["metrics"], cache_path(name)


class _Parser(argparse.ArgumentParser):
    def error(self, message):
        raise ValueError(message)


def main():
    model_name = None
    try:
        parser = _Parser(prog="predict_api.py")
        parser.add_argument("--model", default="lgbm",
                            choices=["lr", "knn", "svm", "dt", "rf", "lgbm"])
        parser.add_argument("--budget", type=float, default=12.0, help="集数（episodes）")
        parser.add_argument("--popularity", type=float, default=10000.0, help="收藏数 popularity")
        parser.add_argument("--runtime", type=int, default=24, help="单集时长（分钟）")
        parser.add_argument("--language", default="ja", help="原始语言代码，如 ja/en")
        parser.add_argument("--status", default="TV", help="类型，如 TV/OVA/ONA/Movie")
        parser.add_argument("--train", action="store_true", help="仅训练并缓存模型后退出")
        args = parser.parse_args()

        model_name = args.model
        if args.train:
            met = train_and_cache(model_name)
            print(json.dumps({
                "model": model_name,
                "trained": True,
                "cache_file": cache_path(model_name),
                "metrics": met,
            }, ensure_ascii=False))
            sys.exit(0)

        pred, trained_now, met, cfile = predict(
            model_name, args.budget, args.popularity, args.runtime,
            args.language, args.status,
        )
        print(json.dumps({
            "model": model_name,
            "prediction": pred,
            "currency": "members",
            "trained_now": trained_now,
            "cache_file": cfile,
            "metrics": met,
        }, ensure_ascii=False))
        sys.exit(0)
    except BaseException as exc:  # noqa: BLE001 - 统一转成契约 JSON
        if isinstance(exc, SystemExit) and exc.code == 0:
            raise
        print(json.dumps({
            "model": model_name,
            "error": str(exc),
            "trace": traceback.format_exc(),
        }, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    _stdout_reconfigure()
    main()
