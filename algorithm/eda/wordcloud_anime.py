# -*- coding: utf-8 -*-
"""生成题材 / 制作公司 / 制片人 词云图。"""
import os
import pandas as pd
from wordcloud import WordCloud

BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(BASE))
CSV = os.path.join(ROOT, "data", "processed", "anime_train.csv")
OUT = os.path.join(BASE, "figures")
os.makedirs(OUT, exist_ok=True)
FONT = "C:/Windows/Fonts/msyh.ttc"

df = pd.read_csv(CSV)


def make(series, out, cmap):
    words = []
    for v in series.dropna().astype(str):
        words += [p.strip() for p in v.split(",") if p.strip() and p.strip() != "UNKNOWN"]
    text = " ".join(words)
    wc = WordCloud(width=1000, height=600, background_color="#fffafc",
                   font_path=FONT, colormap=cmap, max_words=120).generate(text)
    wc.to_file(os.path.join(OUT, out))
    print("ok", out)


make(df["genres"], "fig20_genre_wordcloud.png", "RdPu")
make(df["studios"], "fig21_studio_wordcloud.png", "PuBu")
make(df["producers"], "fig22_producer_wordcloud.png", "OrRd")
