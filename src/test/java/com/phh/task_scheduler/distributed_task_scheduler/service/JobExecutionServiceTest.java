package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import com.phh.task_scheduler.distributed_task_scheduler.worker.JobWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private TaskExecutor jobTaskExecutor;

	@Mock
	private JobWorker jobWorker;

	@InjectMocks
	private JobExecutionService jobExecutionService;

	@Test
	void shouldSubmitDueJobsForExecution() {

		Job job1 = createJob(1L);
		Job job2 = createJob(2L);

		when(jobRepository.findDueJobs(
				eq(List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				)),
				any(Instant.class)
		)).thenReturn(List.of(job1, job2));

		jobExecutionService.executeDueJobs();

		verify(jobTaskExecutor, times(2))
				.execute(any(Runnable.class));
	}

	@Test
	void shouldExecuteSubmittedTasksWithCorrectJobIds() {

		Job job1 = createJob(1L);
		Job job2 = createJob(2L);

		when(jobRepository.findDueJobs(
				any(),
				any(Instant.class)
		)).thenReturn(List.of(job1, job2));

		List<Runnable> tasks = new ArrayList<>();

		doAnswer(invocation -> {
			tasks.add(invocation.getArgument(0));
			return null;
		}).when(jobTaskExecutor)
				.execute(any(Runnable.class));

		jobExecutionService.executeDueJobs();

		assertThat(tasks).hasSize(2);

		tasks.get(0).run();
		tasks.get(1).run();

		verify(jobWorker).execute(1L);
		verify(jobWorker).execute(2L);
	}

	@Test
	void shouldNotSubmitAnythingWhenThereAreNoDueJobs() {

		when(jobRepository.findDueJobs(
				any(),
				any(Instant.class)
		)).thenReturn(List.of());

		jobExecutionService.executeDueJobs();

		verifyNoInteractions(jobTaskExecutor);
		verifyNoInteractions(jobWorker);
	}

	private Job createJob(Long id) {

		Job job = new Job(
				"test-job-" + id,
				JobType.SEND_EMAIL,
				Instant.now().plusSeconds(60),
				3
		);

		job.setId(id);

		return job;
	}
}