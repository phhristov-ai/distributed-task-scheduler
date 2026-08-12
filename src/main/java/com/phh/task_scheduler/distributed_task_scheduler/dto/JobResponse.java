package com.phh.task_scheduler.distributed_task_scheduler.dto;

import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;

import java.time.Instant;

public record JobResponse(
		Long id,
		String name,
		JobType jobType,
		JobStatus status,
		Instant executeAt,
		Integer attempts,
		Integer maxAttempts,
		Instant nextAttemptAt,
		String lastError,
		Instant createdAt,
		Instant updatedAt
) {
}