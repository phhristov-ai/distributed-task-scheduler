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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRecoveryServiceTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private JobRetryService jobRetryService;

	@InjectMocks
	private JobRecoveryService jobRecoveryService;

	@Test
	void shouldRecoverExpiredJobs() {

		Job job = createJob();

		Instant expiredLease = Instant.parse(
				"2026-08-11T09:00:00Z"
		);

		job.setLeaseUntil(expiredLease);
		job.setWorkerId("worker-123");

		when(jobRepository.findExpiredJobs(
				eq(JobStatus.RUNNING),
				any(Instant.class)
		)).thenReturn(List.of(job));

		jobRecoveryService.recoverExpiredJobs();

		assertThat(job.getLeaseUntil())
				.isNull();

		assertThat(job.getWorkerId())
				.isNull();

		verify(jobRetryService).retryOrFail(
				eq(job),
				eq("Job lease expired"),
				any(Instant.class)
		);
	}

	@Test
	void shouldDoNothingWhenThereAreNoExpiredJobs() {

		when(jobRepository.findExpiredJobs(
				eq(JobStatus.RUNNING),
				any(Instant.class)
		)).thenReturn(List.of());

		jobRecoveryService.recoverExpiredJobs();

		verify(jobRetryService, never())
				.retryOrFail(
						any(Job.class),
						anyString(),
						any(Instant.class)
				);
	}

	@Test
	void shouldRecoverAllExpiredJobs() {

		Job job1 = createJob();
		Job job2 = createJob();

		job1.setWorkerId("worker-1");
		job2.setWorkerId("worker-2");

		when(jobRepository.findExpiredJobs(
				eq(JobStatus.RUNNING),
				any(Instant.class)
		)).thenReturn(List.of(job1, job2));

		jobRecoveryService.recoverExpiredJobs();

		assertThat(job1.getWorkerId()).isNull();
		assertThat(job2.getWorkerId()).isNull();

		verify(jobRetryService).retryOrFail(
				eq(job1),
				eq("Job lease expired"),
				any(Instant.class)
		);

		verify(jobRetryService).retryOrFail(
				eq(job2),
				eq("Job lease expired"),
				any(Instant.class)
		);
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