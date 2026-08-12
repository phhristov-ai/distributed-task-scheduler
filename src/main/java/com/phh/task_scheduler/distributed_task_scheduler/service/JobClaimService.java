package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobClaimService {

	private final JobRepository jobRepository;
	private final long leaseDurationSeconds;

	public JobClaimService(
			JobRepository jobRepository,
			@Value("${scheduler.worker.lease-duration-seconds:30}")
			long leaseDurationSeconds
	) {
		this.jobRepository = jobRepository;
		this.leaseDurationSeconds = leaseDurationSeconds;
	}

	@Transactional
	public Job claim(Long jobId) {

		Instant now = Instant.now();
		Instant leaseUntil = now.plusSeconds(leaseDurationSeconds);

		String workerId = UUID.randomUUID().toString();

		int claimed = jobRepository.claimJob(
				jobId,
				JobStatus.RUNNING,
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				leaseUntil,
				workerId,
				now
		);

		if (claimed == 0) {
			return null;
		}

		return jobRepository.findById(jobId)
				.orElseThrow(() ->
						new IllegalStateException(
								"Job disappeared after being claimed: " + jobId
						)
				);
	}
}