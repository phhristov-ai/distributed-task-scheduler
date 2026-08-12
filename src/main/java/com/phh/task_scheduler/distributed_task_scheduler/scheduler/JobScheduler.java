package com.phh.task_scheduler.distributed_task_scheduler.scheduler;

import com.phh.task_scheduler.distributed_task_scheduler.service.JobExecutionService;
import com.phh.task_scheduler.distributed_task_scheduler.service.JobRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {

	private final JobExecutionService jobExecutionService;

	private final JobRecoveryService jobRecoveryService;

	@Scheduled(fixedDelay = 1000)
	public void scheduleJobs() {

		log.debug("Checking for scheduled jobs");

		jobExecutionService.executeDueJobs();
	}

	@Scheduled(fixedDelay = 10_000)
	public void recoverJobs() {
		jobRecoveryService.recoverExpiredJobs();
	}
}