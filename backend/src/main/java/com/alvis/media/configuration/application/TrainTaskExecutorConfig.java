package com.alvis.media.configuration.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 模型训练专用线程池。
 *
 * 训练是CPU 密集型长耗时任务（RF / LightGBM 秒级以上），
 * 必须与 Web 请求线程池隔离，否则慢训练会占满 Tomcat/Undertow 工作线程导致接口全站卡死；
 * 有界队列 + CallerRuns 之外的拒绝策略为 AbortPolicy，队列满时快速失败并让任务保持 PENDING。
 */
@Configuration
public class TrainTaskExecutorConfig {

    public static final String TRAIN_TASK_EXECUTOR = "trainTaskExecutor";

    @Bean(name = TRAIN_TASK_EXECUTOR)
    public Executor trainTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(32);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("train-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
