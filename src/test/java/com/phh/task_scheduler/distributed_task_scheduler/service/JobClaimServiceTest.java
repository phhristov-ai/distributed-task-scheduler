package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JobClaimServiceTest {

	private JobRepository jobRepository;
	private JobClaimService jobClaimService;

	@BeforeEach
	void setUp() {
		jobRepository = mock(JobRepository.class);

		jobClaimService = new JobClaimService(
				jobRepository,
				30
		);
	}

	@Test
	void shouldClaimJob() {

		Long jobId = 1L;

		Job job = createJob();

		when(jobRepository.claimJob(
				eq(jobId),
				eq(JobStatus.RUNNING),
				eq(List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				)),
				any(Instant.class),
				anyString(),
				any(Instant.class)
		)).thenReturn(1);

		when(jobRepository.findById(jobId))
				.thenReturn(Optional.of(job));

		Job result = jobClaimService.claim(jobId);

		assertThat(result)
				.isSameAs(job);

		verify(jobRepository).claimJob(
				eq(jobId),
				eq(JobStatus.RUNNING),
				eq(List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				)),
				any(Instant.class),
				anyString(),
				any(Instant.class)
		);

		verify(jobRepository)
				.findById(jobId);
	}

	@Test
	void shouldReturnNullWhenJobCannotBeClaimed() {

		Long jobId = 1L;

		when(jobRepository.claimJob(
				eq(jobId),
				eq(JobStatus.RUNNING),
				eq(List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				)),
				any(Instant.class),
				anyString(),
				any(Instant.class)
		)).thenReturn(0);

		Job result = jobClaimService.claim(jobId);

		assertThat(result)
				.isNull();

		verify(jobRepository, never())
				.findById(anyLong());
	}

	@Test
	void shouldThrowWhenJobWasClaimedButCannotBeFound() {

		Long jobId = 1L;

		when(jobRepository.claimJob(
				eq(jobId),
				eq(JobStatus.RUNNING),
				eq(List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				)),
				any(Instant.class),
				anyString(),
				any(Instant.class)
		)).thenReturn(1);

		when(jobRepository.findById(jobId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> jobClaimService.claim(jobId)
		)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage(
						"Job disappeared after being claimed: " + jobId
				);
	}

	private Job createJob() {

		return new Job(
				"Test job",
				JobType.SEND_EMAIL,
				Instant.now().plusSeconds(3600),
				3
		);
	}
}