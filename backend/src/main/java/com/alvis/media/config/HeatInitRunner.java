package com.alvis.media.config;

import com.alvis.media.service.VideoInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后自动触发热度计算（heat_score），
 * 避免首页「视频热度榜单 TOP10」因 heat_score 为 NULL 而永远为空。
 *
 * <p>可通过配置项 {@code anime.heat.init-on-startup} 关闭。
 * 开启时每次启动都会重算全库 24661 条，会明显拖慢启动；
 * 只需要在首次导入数据后或热度公式变更后重算一次，其余情况建议置为 false。
 */
@Slf4j
@Component
public class HeatInitRunner implements CommandLineRunner {

    private final VideoInfoService videoInfoService;

    /** 是否在启动时重算全库热度 */
    @Value("${anime.heat.init-on-startup:true}")
    private boolean initOnStartup;

    public HeatInitRunner(VideoInfoService videoInfoService) {
        this.videoInfoService = videoInfoService;
    }

    @Override
    public void run(String... args) {
        if (!initOnStartup) {
            log.info("已跳过启动时热度重算（anime.heat.init-on-startup=false），如需重算请调用 /api/admin/dashboard/recalculate-heat");
            return;
        }
        try {
            int n = videoInfoService.recalculateHeat();
            log.info("视频热度初始化完成，共更新 {} 部电影", n);
        } catch (Exception e) {
            log.warn("视频热度初始化失败（可能尚未导入数据），已跳过", e);
        }
    }
}
