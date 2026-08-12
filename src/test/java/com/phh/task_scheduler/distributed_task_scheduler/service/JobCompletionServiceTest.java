package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCompletionServiceTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private JobRetryService jobRetryService;

	@InjectMocks
	private JobCompletionService jobCompletionService;

	@Test
	void shouldMarkJobAsCompleted() {

		Long jobId = 1L;

		Job job = createJob();

		job.setStatus(JobStatus.RUNNING);
		job.setLastError("Previous error");
		job.setLeaseUntil(
				Instant.parse("2026-08-11T10:00:30Z")
		);
		job.setWorkerId("worker-123");

		when(jobRepository.findById(jobId))
				.thenReturn(Optional.of(job));

		jobCompletionService.markCompleted(jobId);

		assertThat(job.getStatus())
				.isEqualTo(JobStatus.COMPLETED);

		assertThat(job.getLastError())
				.isNull();

		assertThat(job.getLeaseUntil())
				.isNull();

		assertThat(job.getWorkerId())
				.isNull();

		verify(jobRepository)
				.findById(jobId);
	}

	@Test
	void shouldHandleFailure() {

		Long jobId = 1L;

		Job job = createJob();

		job.setStatus(JobStatus.RUNNING);
		job.setLeaseUntil(
				Instant.parse("2026-08-11T10:00:30Z")
		);
		job.setWorkerId("worker-123");

		Exception exception =
				new RuntimeException("Something failed");

		when(jobRepository.findById(jobId))
				.thenReturn(Optional.of(job));

		jobCompletionService.handleFailure(
				jobId,
				exception
		);

		verify(jobRetryService).retryOrFail(
				eq(job),
				eq("Something failed"),
				any(Instant.class)
		);
	}

	@Test
	void shouldThrowWhenJobDoesNotExist() {

		Long jobId = 999L;

		when(jobRepository.findById(jobId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> jobCompletionService.markCompleted(jobId)
		)
				.isInstanceOf(NoSuchElementException.class);

		verify(jobRepository)
				.findById(jobId);

		verifyNoInteractions(jobRetryService);
	}

	private Job createJob() {

		return new Job(
				"Test job",
				JobType.SEND_EMAIL,
				Instant.parse("2026-08-11T11:00:00Z"),
				3
		);
	}
}