package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.dto.CreateJobRequest;
import com.phh.task_scheduler.distributed_task_scheduler.dto.JobResponse;
import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {

	private final JobRepository jobRepository;

	@Transactional
	public JobResponse createJob(CreateJobRequest request) {

		Job job = new Job(
				request.name(),
				request.jobType(),
				request.executeAt(),
				request.maxAttempts()
		);

		Job savedJob = jobRepository.save(job);

		return toResponse(savedJob);
	}

	@Transactional(readOnly = true)
	public JobResponse getJob(Long id) {

		Job job = jobRepository.findById(id)
				.orElseThrow(() ->
						new IllegalArgumentException("Job not found")
				);

		return toResponse(job);
	}

	private JobResponse toResponse(Job job) {

		return new JobResponse(
				job.getId(),
				job.getName(),
				job.getType(),
				job.getStatus(),
				job.getExecuteAt(),
				job.getAttempts(),
				job.getMaxAttempts(),
				job.getNextAttemptAt(),
				job.getLastError(),
				job.getCreatedAt(),
				job.getUpdatedAt()
		);
	}
}