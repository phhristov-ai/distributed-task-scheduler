package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class JobRetryService {

	private static final long INITIAL_DELAY_SECONDS = 2;
	private static final long MAX_DELAY_SECONDS = 60;

	public void retryOrFail(
			Job job,
			String errorMessage,
			Instant now
	) {

		job.setLastError(errorMessage);

		if (hasAttemptsRemaining(job)) {
			scheduleRetry(job, now);
		} else {
			markFailed(job);
		}
	}

	private boolean hasAttemptsRemaining(Job job) {

		return job.getAttempts() < job.getMaxAttempts();
	}

	private void scheduleRetry(
			Job job,
			Instant now
	) {

		job.setStatus(JobStatus.RETRYING);
		job.setNextAttemptAt(
				calculateNextAttempt(
						job.getAttempts(),
						now
				)
		);

		log.warn(
				"Job scheduled for retry: id={}, attempt={}, nextAttemptAt={}",
				job.getId(),
				job.getAttempts(),
				job.getNextAttemptAt()
		);
	}

	private void markFailed(Job job) {

		job.setStatus(JobStatus.FAILED);

		log.error(
				"Job permanently failed: id={}, attempts={}",
				job.getId(),
				job.getAttempts()
		);
	}

	private Instant calculateNextAttempt(
			int attempts,
			Instant now
	) {

		long delaySeconds = INITIAL_DELAY_SECONDS * (1L << (attempts - 1));

		delaySeconds = Math.min(
				delaySeconds,
				MAX_DELAY_SECONDS
		);

		return now.plusSeconds(delaySeconds);
	}
}