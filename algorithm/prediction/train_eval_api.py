# -*- coding: utf-8 -*-
"""
train_eval_api.py
==================
模型训练 + 评估命令行封装，供 Java 后端「数据集/模型管理」模块通过 ProcessBuilder 调用。

与 predict_api.py 的区别：
    predict_api.py     面向单条预测，训练结果只是副产物；
    train_eval_api.py  面向模型管理，显式训练 + 显式评估，输出完整指标供落库。

支持的算法（热度回归，标签 = revenue 成员数）：
    lr / knn / svm / dt / rf / lgbm

用法：
    python train_eval_api.py --model rf
    python train_eval_api.py --model lgbm --dataset ../../data/train.csv
    python train_eval_api.py --model rf --eval-only
"""

import argparse
import contextlib
import io
import json
import os
import sys
import time
import traceback
import warnings

warnings.filterwarnings("ignore")

# 复用 predict_api 的清洗/特征工程/建模/评估实现，保证训练与预测口径一致
BASE = os.path.dirname(os.path.abspath(__file__))
if BASE not in sys.path:
    sys.path.insert(0, BASE)

import predict_api  # noqa: E402


class _Parser(argparse.ArgumentParser):
    def error(self, message):
        raise ValueError(message)


def _silent():
    stack = contextlib.ExitStack()
    stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
    stack.enter_context(contextlib.redirect_stderr(io.StringIO()))
    return stack


def resolve_dataset(dataset):
    if dataset:
        path = os.path.abspath(dataset)
        if not os.path.exists(path):
            raise FileNotFoundError("数据集文件不存在: %s" % path)
        return path
    path = predict_api.TRAIN_CSV
    if not os.path.exists(path):
        raise FileNotFoundError("默认数据集不存在: %s" % path)
    return path


def count_rows(csv_path):
    with open(csv_path, "rb") as fh:
        return max(0, sum(1 for _ in fh) - 1)


def train_and_eval(model_name, dataset=None, eval_only=False):
    """训练（或直接加载缓存）并在测试集上评估，返回可 JSON 序列化的结果字典。"""
    import joblib
    from sklearn.model_selection import train_test_split

    csv_path = resolve_dataset(dataset)
    rows = count_rows(csv_path)
    cache_file = predict_api.cache_path(model_name)
    start = time.time()
    trained = False

    if eval_only:
        if not os.path.exists(cache_file):
            raise FileNotFoundError("模型缓存不存在，请先训练: %s" % cache_file)
        with _silent():
            bundle = joblib.load(cache_file)
        if bundle.get("version") != predict_api.FEATURE_VERSION:
            raise RuntimeError("缓存版本过期，请重新训练")
        metrics = bundle["metrics"]
    else:
        # 复用 predict_api 数据管线；临时替换其数据集常量以支持自定义数据集
        original_csv = predict_api.TRAIN_CSV
        try:
            predict_api.TRAIN_CSV = csv_path
            with _silent():
                X, y_log, y_raw, _meta = predict_api.load_and_prepare()
                X_train, X_test, yl_train, yl_test, yr_train, yr_test = train_test_split(
                    X, y_log, y_raw, test_size=0.2, random_state=42
                )
                model = predict_api.build_model(model_name, X_train, yl_train)
                metrics = predict_api.calc_metrics(model, X_test, yl_test, yr_test)
                os.makedirs(predict_api.CACHE_DIR, exist_ok=True)
                joblib.dump({
                    "model": model,
                    "metrics": metrics,
                    "feature_names": predict_api.FEATURE_NAMES,
                    "version": predict_api.FEATURE_VERSION,
                    "target": "log1p",
                }, cache_file)
        finally:
            predict_api.TRAIN_CSV = original_csv
        trained = True

    return {
        "model": model_name,
        "algo_type": "regression",
        "rows": rows,
        "trained": trained,
        "train_seconds": round(time.time() - start, 3),
        "data_source": csv_path,
        "cache_file": cache_file,
        "feature_version": predict_api.FEATURE_VERSION,
        "metrics": metrics,
    }


def main():
    model_name = None
    try:
        parser = _Parser(prog="train_eval_api.py")
        parser.add_argument("--model", default="rf",
                            choices=["lr", "knn", "svm", "dt", "rf", "lgbm"],
                            help="待训练的回归算法")
        parser.add_argument("--dataset", default=None, help="数据集 CSV 路径，缺省用 data/train.csv")
        parser.add_argument("--eval-only", action="store_true", help="只用缓存模型评估，不重新训练")
        args = parser.parse_args()
        model_name = args.model
        result = train_and_eval(model_name, args.dataset, args.eval_only)
        print(json.dumps(result, ensure_ascii=False))
        sys.exit(0)
    except BaseException as exc:
        if isinstance(exc, SystemExit) and exc.code == 0:
            raise
        print(json.dumps({
            "model": model_name,
            "error": str(exc),
            "trace": traceback.format_exc(),
        }, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    for _s in (sys.stdout, sys.stderr):
        try:
            _s.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass
    main()
