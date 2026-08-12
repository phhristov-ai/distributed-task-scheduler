package com.phh.task_scheduler.distributed_task_scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfig {

	@Bean
	public TaskExecutor jobTaskExecutor() {

		ThreadPoolTaskExecutor executor =
				new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("job-worker-");

		executor.initialize();

		return executor;
	}
}