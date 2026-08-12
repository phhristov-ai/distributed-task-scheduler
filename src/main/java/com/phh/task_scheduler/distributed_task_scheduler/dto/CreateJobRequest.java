package com.phh.task_scheduler.distributed_task_scheduler.dto;

import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record CreateJobRequest(

		@NotBlank
		String name,

		@NotNull
		JobType jobType,

		@NotNull
		@Future
		Instant executeAt,

		@Min(1)
		@Max(10)
		@NotNull
		Integer maxAttempts
) {
}