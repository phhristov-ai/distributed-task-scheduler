package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.dto.CreateJobRequest;
import com.phh.task_scheduler.distributed_task_scheduler.dto.JobResponse;
import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

	@Mock
	private JobRepository jobRepository;

	@InjectMocks
	private JobService jobService;

	@Test
	void createJob_shouldCreateAndReturnJob() {

		Instant executeAt = Instant.now().plusSeconds(3600);

		CreateJobRequest request = new CreateJobRequest(
				"Send email",
				JobType.SEND_EMAIL,
				executeAt,
				3
		);

		Job savedJob = new Job(
				"Send email",
				JobType.SEND_EMAIL,
				executeAt,
				3
		);

		// simulate the database assigning the ID
		ReflectionTestUtils.setField(savedJob, "id", 1L);

		when(jobRepository.save(any(Job.class)))
				.thenReturn(savedJob);

		JobResponse response = jobService.createJob(request);

		assertEquals(1L, response.id());
		assertEquals("Send email", response.name());
		assertEquals(JobType.SEND_EMAIL, response.jobType());
		assertEquals(JobStatus.PENDING, response.status());
		assertEquals(executeAt, response.executeAt());
		assertEquals(0, response.attempts());
		assertEquals(3, response.maxAttempts());
		assertEquals(executeAt, response.nextAttemptAt());

		verify(jobRepository).save(any(Job.class));
	}

	@Test
	void createJob_shouldCreateCorrectJob() {

		Instant executeAt = Instant.now().plusSeconds(3600);

		CreateJobRequest request = new CreateJobRequest(
				"Send email",
				JobType.SEND_EMAIL,
				executeAt,
				3
		);

		ArgumentCaptor<Job> captor =
				ArgumentCaptor.forClass(Job.class);

		Job savedJob = new Job(
				"Send email",
				JobType.SEND_EMAIL,
				executeAt,
				3
		);

		ReflectionTestUtils.setField(savedJob, "id", 1L);

		when(jobRepository.save(any(Job.class)))
				.thenReturn(savedJob);

		JobResponse response = jobService.createJob(request);

		verify(jobRepository).save(captor.capture());

		Job job = captor.getValue();

		assertEquals("Send email", job.getName());
		assertEquals(JobType.SEND_EMAIL, job.getType());
		assertEquals(JobStatus.PENDING, job.getStatus());
		assertEquals(executeAt, job.getExecuteAt());
		assertEquals(executeAt, job.getNextAttemptAt());
		assertEquals(0, job.getAttempts());
		assertEquals(3, job.getMaxAttempts());

		assertEquals(1L, response.id());
	}

	@Test
	void getJob_shouldReturnJob() {

		Instant executeAt = Instant.now().plusSeconds(3600);

		Job job = new Job(
				"Send email",
				JobType.SEND_EMAIL,
				executeAt,
				3
		);

		ReflectionTestUtils.setField(job, "id", 42L);

		when(jobRepository.findById(42L))
				.thenReturn(Optional.of(job));

		JobResponse response = jobService.getJob(42L);

		assertEquals(42L, response.id());
		assertEquals("Send email", response.name());
		assertEquals(JobType.SEND_EMAIL, response.jobType());
		assertEquals(JobStatus.PENDING, response.status());

		verify(jobRepository).findById(42L);
	}

	@Test
	void getJob_shouldThrowWhenJobDoesNotExist() {

		when(jobRepository.findById(999L))
				.thenReturn(Optional.empty());

		IllegalArgumentException exception =
				assertThrows(
						IllegalArgumentException.class,
						() -> jobService.getJob(999L)
				);

		assertEquals("Job not found", exception.getMessage());

		verify(jobRepository).findById(999L);
	}
}
