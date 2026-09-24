# -*- coding: utf-8 -*-
"""
NCF.py —— 神经协同过滤（Neural Collaborative Filtering）
=========================================================
对应《03-算法设计说明书》A7：NeuMF（GMF + MLP 融合）深度学习推荐模型。

模型结构（NeuMF）:
    ┌── GMF 分支:  p_u ⊙ q_i                        (gmf_dim = 32)
    └── MLP 分支:  MLP([p'_u ; q'_i]) → 64→32→16→8  (mlp_dim = 32)
    融合:  concat[GMF(32); MLP(8)] → Linear(40→1) → 评分残差
    最终:  r̂ = global_mean + 残差

数据:
    data/recommendation/personal/train.csv  (userId, movieId, rating)

缓存:
    algorithm/recommendation/deep_recommender/model_cache/ncf.pt
    （首次调用训练并缓存，之后加载秒级推理）

用法（独立调试）:
    python NCF.py --user 15 --top 10
"""

import argparse
import copy
import os
import sys

import numpy as np
import pandas as pd
import torch
import torch.nn as nn

# ---------------------------------------------------------------------------
# 路径（全部基于脚本自身位置，与 CWD 无关）
# ---------------------------------------------------------------------------
BASE = os.path.dirname(os.path.abspath(__file__))
SYSTEM_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(BASE)))
PERSONAL_DIR = os.path.join(SYSTEM_ROOT, "data", "recommendation", "personal")
TRAIN_CSV = os.path.join(PERSONAL_DIR, "train.csv")
CACHE_DIR = os.path.join(BASE, "model_cache")
CACHE_FILE = os.path.join(CACHE_DIR, "ncf.pt")

# ---------------------------------------------------------------------------
# 超参数（与《算法设计说明书》4.4 节一致）
# ---------------------------------------------------------------------------
GMF_DIM = 32
MLP_DIM = 32
MLP_LAYERS = (64, 32, 16, 8)
DROPOUT = 0.2
BATCH_SIZE = 256
EPOCHS = 15
LR = 1e-3
WEIGHT_DECAY = 1e-5
PATIENCE = 3          # 早停耐心值
VAL_RATIO = 0.1       # 从训练集再切 10% 作验证集
SEED = 42


class NeuMF(nn.Module):
    """NeuMF：GMF 与 MLP 双分支融合的神经协同过滤模型。"""

    def __init__(self, n_users, n_items):
        super().__init__()
        # GMF 分支
        self.user_emb_gmf = nn.Embedding(n_users, GMF_DIM)
        self.item_emb_gmf = nn.Embedding(n_items, GMF_DIM)
        # MLP 分支
        self.user_emb_mlp = nn.Embedding(n_users, MLP_DIM)
        self.item_emb_mlp = nn.Embedding(n_items, MLP_DIM)

        layers = []
        in_dim = MLP_DIM * 2
        for out_dim in MLP_LAYERS:
            layers += [nn.Linear(in_dim, out_dim), nn.ReLU(), nn.Dropout(DROPOUT)]
            in_dim = out_dim
        self.mlp = nn.Sequential(*layers)

        # 融合输出层
        self.out = nn.Linear(GMF_DIM + MLP_LAYERS[-1], 1)

        for emb in (self.user_emb_gmf, self.item_emb_gmf,
                    self.user_emb_mlp, self.item_emb_mlp):
            nn.init.normal_(emb.weight, std=0.01)

    def forward(self, user_idx, item_idx):
        # GMF：逐元素积
        gmf = self.user_emb_gmf(user_idx) * self.item_emb_gmf(item_idx)
        # MLP：拼接后全连接
        mlp_in = torch.cat([self.user_emb_mlp(user_idx),
                            self.item_emb_mlp(item_idx)], dim=-1)
        mlp = self.mlp(mlp_in)
        # 融合
        x = torch.cat([gmf, mlp], dim=-1)
        return self.out(x).squeeze(-1)


class NCF_recommender:
    """NCF 推荐器：封装数据映射、训练、缓存与推理。"""

    def __init__(self):
        torch.manual_seed(SEED)
        np.random.seed(SEED)
        self.device = torch.device("cpu")
        self.user2idx = {}
        self.item2idx = {}
        self.global_mean = 0.0
        self.model = None
        self.metrics = {}
        if os.path.exists(CACHE_FILE):
            self._load()
        else:
            self._train_and_save()

    # ---------------- 数据 ----------------
    @staticmethod
    def _read_train():
        df = pd.read_csv(TRAIN_CSV)[["userId", "movieId", "rating"]].dropna()
        df["userId"] = df["userId"].astype(int)
        df["movieId"] = df["movieId"].astype(int)
        return df

    # ---------------- 训练 ----------------
    def _train_and_save(self):
        df = self._read_train()
        users = sorted(df["userId"].unique())
        items = sorted(df["movieId"].unique())
        self.user2idx = {int(u): i for i, u in enumerate(users)}
        self.item2idx = {int(m): i for i, m in enumerate(items)}
        self.global_mean = float(df["rating"].mean())

        u_all = df["userId"].map(self.user2idx).to_numpy(dtype=np.int64)
        i_all = df["movieId"].map(self.item2idx).to_numpy(dtype=np.int64)
        y_all = (df["rating"].to_numpy(dtype=np.float32) - self.global_mean)

        rng = np.random.RandomState(SEED)
        idx = np.arange(len(df))
        rng.shuffle(idx)
        n_val = max(1, int(len(idx) * VAL_RATIO))
        val_idx, tr_idx = idx[:n_val], idx[n_val:]

        x_u = torch.tensor(u_all, dtype=torch.long)
        x_i = torch.tensor(i_all, dtype=torch.long)
        x_y = torch.tensor(y_all, dtype=torch.float32)

        self.model = NeuMF(len(users), len(items)).to(self.device)
        optimizer = torch.optim.Adam(self.model.parameters(),
                                     lr=LR, weight_decay=WEIGHT_DECAY)
        loss_fn = nn.MSELoss()

        best_val = float("inf")
        best_state = copy.deepcopy(self.model.state_dict())
        bad_epochs = 0

        for _epoch in range(EPOCHS):
            self.model.train()
            perm = np.random.permutation(tr_idx)
            for start in range(0, len(perm), BATCH_SIZE):
                batch = perm[start:start + BATCH_SIZE]
                optimizer.zero_grad()
                pred = self.model(x_u[batch], x_i[batch])
                loss = loss_fn(pred, x_y[batch])
                loss.backward()
                optimizer.step()

            # 验证集 RMSE（还原到原始评分量纲）
            self.model.eval()
            with torch.no_grad():
                val_pred = self.model(x_u[val_idx], x_i[val_idx]).numpy()
            val_rmse = float(np.sqrt(np.mean(
                ((val_pred + self.global_mean) -
                 df["rating"].to_numpy(dtype=np.float32)[val_idx]) ** 2)))
            if val_rmse < best_val - 1e-4:
                best_val = val_rmse
                best_state = copy.deepcopy(self.model.state_dict())
                bad_epochs = 0
            else:
                bad_epochs += 1
                if bad_epochs >= PATIENCE:
                    break

        self.model.load_state_dict(best_state)
        self.metrics = {"val_rmse": round(best_val, 4), "n_users": len(users),
                        "n_items": len(items), "global_mean": round(self.global_mean, 4)}

        os.makedirs(CACHE_DIR, exist_ok=True)
        torch.save({
            "state_dict": self.model.state_dict(),
            "user2idx": self.user2idx,
            "item2idx": self.item2idx,
            "global_mean": self.global_mean,
            "metrics": self.metrics,
            "n_users": len(users),
            "n_items": len(items),
        }, CACHE_FILE)

    # ---------------- 加载 ----------------
    def _load(self):
        bundle = torch.load(CACHE_FILE, map_location=self.device, weights_only=False)
        self.user2idx = bundle["user2idx"]
        self.item2idx = bundle["item2idx"]
        self.global_mean = bundle["global_mean"]
        self.metrics = bundle.get("metrics", {})
        self.model = NeuMF(bundle["n_users"], bundle["n_items"]).to(self.device)
        self.model.load_state_dict(bundle["state_dict"])
        self.model.eval()

    # ---------------- 推理 ----------------
    def recommend(self, user_id, candidate_ids, top=10):
        """对候选动漫批量打分并返回 Top-K。

        返回: [(movieId:int, predicted_rating:float), ...]
        """
        user_id = int(user_id)
        if user_id not in self.user2idx:
            raise ValueError(
                "NCF: 用户 %s 不在训练集用户列表中（可用用户示例：%s）"
                % (user_id, sorted(self.user2idx.keys())[:5]))
        cand = [int(c) for c in candidate_ids if int(c) in self.item2idx]
        if not cand:
            return []
        self.model.eval()
        u_idx = self.user2idx[user_id]
        uu = torch.full((len(cand),), u_idx, dtype=torch.long)
        ii = torch.tensor([self.item2idx[c] for c in cand], dtype=torch.long)
        with torch.no_grad():
            pred = self.model(uu, ii).numpy() + self.global_mean
        order = np.argsort(-pred)[:top]
        return [(cand[k], float(pred[k])) for k in order]


# ---------------------------------------------------------------------------
# 独立运行（调试用）
# ---------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(prog="NCF.py")
    parser.add_argument("--user", type=int, required=True)
    parser.add_argument("--top", type=int, default=10)
    args = parser.parse_args()

    rec = NCF_recommender()
    df = rec._read_train()
    rated = set(df[df["userId"] == args.user]["movieId"].astype(int))
    counts = df.groupby("movieId")["rating"].count().sort_values(ascending=False)
    cands = [int(m) for m in counts.index if int(m) not in rated][:1000]
    pairs = rec.recommend(args.user, cands, args.top)
    for mid, score in pairs:
        print("movieId=%s  predicted=%.3f" % (mid, score))
    print("metrics:", rec.metrics)


if __name__ == "__main__":
    sys.exit(main())
