package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JobCompletionService {

	private final JobRepository jobRepository;
	private final JobRetryService jobRetryService;

	@Transactional
	public void markCompleted(Long jobId) {

		Job job = getJob(jobId);

		job.setStatus(JobStatus.COMPLETED);
		job.setLastError(null);
		job.setLeaseUntil(null);
		job.setWorkerId(null);
	}

	@Transactional
	public void handleFailure(
			Long jobId,
			Exception exception
	) {

		Job job = getJob(jobId);

		jobRetryService.retryOrFail(
				job,
				exception.getMessage(),
				Instant.now()
		);
	}

	private Job getJob(Long jobId) {

		return jobRepository.findById(jobId)
				.orElseThrow();
	}
}