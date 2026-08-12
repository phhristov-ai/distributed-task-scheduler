package com.phh.task_scheduler.distributed_task_scheduler.worker;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.service.JobClaimService;
import com.phh.task_scheduler.distributed_task_scheduler.service.JobCompletionService;
import com.phh.task_scheduler.distributed_task_scheduler.strategy.JobHandler;
import com.phh.task_scheduler.distributed_task_scheduler.strategy.JobHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobWorker {

	private final JobClaimService jobClaimService;
	private final JobCompletionService jobCompletionService;
	private final JobHandlerRegistry jobHandlerRegistry;

	public void execute(Long jobId) {

		Job job = jobClaimService.claim(jobId);

		if (job == null) {
			log.debug(
					"Job {} was already claimed by another worker",
					jobId
			);
			return;
		}

		logExecution(job);

		try {

			executeJob(job);

			jobCompletionService.markCompleted(job.getId());

		} catch (Exception e) {

			jobCompletionService.handleFailure(
					job.getId(),
					e
			);
		}
	}

	private void executeJob(Job job) {

		JobHandler handler =
				jobHandlerRegistry.getHandler(job.getType());

		handler.execute(job);
	}

	private void logExecution(Job job) {

		log.info(
				"Executing job: id={}, name={}, attempt={}",
				job.getId(),
				job.getName(),
				job.getAttempts()
		);
	}
}