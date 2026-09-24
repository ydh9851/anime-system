<template>
  <div class="app-container eda-page">
    <div class="eda-header">
      <div>
        <h2 class="eda-page-title">动漫数据探索分析（EDA）</h2>
        <p class="eda-page-sub">
          数据源：<code>data/processed/anime_train.csv</code>（5000 条动漫作品，含评分、类型、热度、多语言名称等 26 个字段）
        </p>
      </div>
      <el-tag type="warning" effect="dark" round>Python + pandas + matplotlib</el-tag>
    </div>

    <el-alert
      type="primary"
      :closable="false"
      show-icon
      class="eda-tip"
      title="由 algorithm/eda 下的脚本生成 22 张中文图表与词云；重跑脚本后刷新页面即可看到最新图。"
    />

    <div class="eda-grid">
      <el-card
        v-for="(item, idx) in images"
        :key="item.name"
        shadow="hover"
        class="eda-card fade-up"
        :style="{ animationDelay: (idx * 45) + 'ms' }"
        :body-style="{ padding: '14px' }"
      >
        <div class="eda-index">{{ String(idx + 1).padStart(2, '0') }}</div>
        <el-image
          :src="'/eda/' + item.name"
          :preview-src-list="['/eda/' + item.name]"
          fit="contain"
          class="eda-img"
          lazy
        >
          <template #error>
            <div class="eda-img-error">图片尚未生成</div>
          </template>
        </el-image>
        <div class="eda-title">{{ item.title }}</div>
        <div class="eda-desc">{{ item.desc }}</div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EdaAnalysis',
  data () {
    return {
      images: [
        { name: 'fig01_score_distribution.png', title: '动漫评分分布', desc: '评分整体集中在 5.5~8 分，呈近似正态分布，高分作品占比明显。' },
        { name: 'fig02_members_distribution.png', title: '动漫热度分布（成员数）', desc: '成员数取对数后近似正态，说明热度呈长尾分布，头部作品热度极高。' },
        { name: 'fig03_type_distribution.png', title: '动漫类型数量分布', desc: 'TV 数量最多，OVA、Movie、ONA、Special、Music 依次递减。' },
        { name: 'fig04_genre_top15.png', title: '动漫题材标签 Top15', desc: '喜剧、动作、奇幻、恋爱等题材出现频率最高，是动漫创作的主流方向。' },
        { name: 'fig05_genre_avg_score.png', title: '各题材平均评分 Top15', desc: '筛选样本量≥100 的题材，观察哪些题材更容易获得高评分。' },
        { name: 'fig06_year_trend.png', title: '各年份动漫数量趋势', desc: '1980—2023 年动漫产出数量变化，近年产量显著提升。' },
        { name: 'fig07_top_score.png', title: '评分 Top10 动漫', desc: '按评分排序的头部动漫作品，评分均在 9 分以上。' },
        { name: 'fig08_top_members.png', title: '热度 Top10 动漫', desc: '按成员数排序，反映最受关注的头部番剧。' },
        { name: 'fig09_top_favorites.png', title: '收藏数 Top10 动漫', desc: '收藏数代表核心粉丝的喜爱程度，与热度不完全一致。' },
        { name: 'fig10_source_distribution.png', title: '原作来源分布', desc: '原创、漫画、轻小说、游戏等来源的动漫数量分布。' },
        { name: 'fig11_studios_top15.png', title: '制作公司作品数量 Top15', desc: '高产制作公司排行，反映各公司产能与活跃度。' },
        { name: 'fig12_rating_distribution.png', title: '动漫分级分布', desc: 'PG-13、全年龄、儿童向、R 级等分级占比。' },
        { name: 'fig13_episodes_distribution.png', title: '动漫集数分布', desc: '大多数作品集中在 1~26 集，长篇作品数量较少。' },
        { name: 'fig14_duration_distribution.png', title: '单集时长分布', desc: '常见单集时长约 24 分钟，剧场版与 OVA 时长更长。' },
        { name: 'fig15_score_vs_members.png', title: '评分与热度关系', desc: '评分与成员数整体呈正相关，但高分不等于高热度。' },
        { name: 'fig16_type_score_box.png', title: '各类型评分箱线图', desc: '对比 TV / Movie / OVA / ONA / Special / Music 的评分分布差异。' },
        { name: 'fig17_correlation_heatmap.png', title: '特征相关性热力图', desc: '热度、收藏、人气、评分等数值特征之间的相关性。' },
        { name: 'fig18_premiered_season.png', title: '开播季度分布', desc: '春、夏、秋、冬四个季度的开播作品数量分布。' },
        { name: 'fig19_multilang_names.png', title: '多语言动漫名称示例', desc: '展示日文 / 罗马音、英文、其他语言名称，体现数据集的多语言特性。' },
        { name: 'fig20_genre_wordcloud.png', title: '题材词云', desc: '动漫题材标签词云，字号越大代表该题材作品越多。' },
        { name: 'fig21_studio_wordcloud.png', title: '制作公司词云', desc: '制作公司作品数量词云，快速识别高产动画公司。' },
        { name: 'fig22_producer_wordcloud.png', title: '制片人词云', desc: '主要制片人参与作品数量词云。' }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
.eda-page {
  padding: 20px 24px 28px;
}

.eda-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.eda-page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: 0.5px;
}

.eda-page-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6b7280;

  code {
    padding: 2px 6px;
    border-radius: 4px;
    background: #f3f4f6;
    color: #9b59b6;
    font-size: 12px;
  }
}

.eda-tip {
  margin-bottom: 18px;
  border-radius: 10px;
}

.eda-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1100px) {
  .eda-grid {
    grid-template-columns: 1fr;
  }
}

.eda-card {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 22px 46px rgba(15, 23, 42, 0.14) !important;

    .eda-img {
      transform: scale(1.03);
    }
  }
}

.eda-index {
  position: absolute;
  top: 10px;
  left: 12px;
  z-index: 2;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff6b9d, #9b59b6);
  border-radius: 8px;
  padding: 2px 8px;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.35);
}

.eda-img {
  width: 100%;
  height: 300px;
  border-radius: 12px;
  background: #fafafa;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
}

.eda-img-error {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
  background: #f9fafb;
  border-radius: 10px;
}

.eda-title {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.eda-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #6b7280;
}
</style>
