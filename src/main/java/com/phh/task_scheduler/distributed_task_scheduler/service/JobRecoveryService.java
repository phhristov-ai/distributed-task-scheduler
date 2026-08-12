package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecoveryService {

	private final JobRepository jobRepository;
	private final JobRetryService jobRetryService;

	@Transactional
	public void recoverExpiredJobs() {

		Instant now = Instant.now();

		List<Job> expiredJobs =
				jobRepository.findExpiredJobs(
						JobStatus.RUNNING,
						now
				);

		for (Job job : expiredJobs) {
			recover(job, now);
		}
	}

	private void recover(Job job, Instant now) {

		log.warn(
				"Recovering expired job: id={}, attempts={}",
				job.getId(),
				job.getAttempts()
		);

		job.setLeaseUntil(null);
		job.setWorkerId(null);

		jobRetryService.retryOrFail(
				job,
				"Job lease expired",
				now
		);
	}
}