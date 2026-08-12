package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JobRetryServiceTest {

	private final JobRetryService jobRetryService =
			new JobRetryService();

	@Test
	void shouldScheduleRetryWhenAttemptsRemain() {

		Instant now = Instant.parse("2026-08-11T10:00:00Z");

		Job job = createJob(2, 5);

		jobRetryService.retryOrFail(
				job,
				"Something failed",
				now
		);

		assertThat(job.getStatus())
				.isEqualTo(JobStatus.RETRYING);

		assertThat(job.getLastError())
				.isEqualTo("Something failed");

		assertThat(job.getNextAttemptAt())
				.isEqualTo(
						Instant.parse("2026-08-11T10:00:04Z")
				);
	}

	@Test
	void shouldFailWhenNoAttemptsRemain() {

		Instant now = Instant.parse("2026-08-11T10:00:00Z");

		Job job = createJob(5, 5);

		jobRetryService.retryOrFail(
				job,
				"Something failed",
				now
		);

		assertThat(job.getStatus())
				.isEqualTo(JobStatus.FAILED);

		assertThat(job.getLastError())
				.isEqualTo("Something failed");
	}

	@Test
	void shouldScheduleFirstRetryAfterTwoSeconds() {

		Instant now = Instant.parse("2026-08-11T10:00:00Z");

		Job job = createJob(1, 5);

		jobRetryService.retryOrFail(
				job,
				"Something failed",
				now
		);

		assertThat(job.getNextAttemptAt())
				.isEqualTo(
						Instant.parse("2026-08-11T10:00:02Z")
				);
	}

	@Test
	void shouldCapRetryDelayAt60Seconds() {

		Instant now = Instant.parse("2026-08-11T10:00:00Z");

		Job job = createJob(10, 20);

		jobRetryService.retryOrFail(
				job,
				"Something failed",
				now
		);

		assertThat(job.getNextAttemptAt())
				.isEqualTo(
						Instant.parse("2026-08-11T10:01:00Z")
				);
	}

	private Job createJob(int attempts, int maxAttempts) {

		Job job = new Job(
				"Test job",
				JobType.SEND_EMAIL,
				Instant.parse("2026-08-11T11:00:00Z"),
				maxAttempts
		);

		job.setAttempts(attempts);

		return job;
	}
}