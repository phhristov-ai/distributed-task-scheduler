package com.phh.task_scheduler.distributed_task_scheduler.worker;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import com.phh.task_scheduler.distributed_task_scheduler.service.JobClaimService;
import com.phh.task_scheduler.distributed_task_scheduler.service.JobCompletionService;
import com.phh.task_scheduler.distributed_task_scheduler.strategy.JobHandler;
import com.phh.task_scheduler.distributed_task_scheduler.strategy.JobHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobWorkerTest {

	@Mock
	private JobClaimService jobClaimService;

	@Mock
	private JobCompletionService jobCompletionService;

	@Mock
	private JobHandlerRegistry jobHandlerRegistry;

	@Mock
	private JobHandler jobHandler;

	@InjectMocks
	private JobWorker jobWorker;

	@Test
	void shouldDoNothingWhenJobCannotBeClaimed() {

		when(jobClaimService.claim(1L))
				.thenReturn(null);

		jobWorker.execute(1L);

		verifyNoInteractions(jobHandlerRegistry);
		verifyNoInteractions(jobCompletionService);
	}

	@Test
	void shouldExecuteJobAndMarkItCompleted() {

		Job job = createJob(1L);

		when(jobClaimService.claim(1L))
				.thenReturn(job);

		when(jobHandlerRegistry.getHandler(job.getType()))
				.thenReturn(jobHandler);

		jobWorker.execute(1L);

		verify(jobHandler)
				.execute(job);

		verify(jobCompletionService)
				.markCompleted(1L);

		verify(jobCompletionService, never())
				.handleFailure(anyLong(), any());
	}

	@Test
	void shouldHandleFailureWhenJobExecutionThrowsException() {

		Job job = createJob(1L);

		RuntimeException exception =
				new RuntimeException("Something went wrong");

		when(jobClaimService.claim(1L))
				.thenReturn(job);

		when(jobHandlerRegistry.getHandler(job.getType()))
				.thenReturn(jobHandler);

		doThrow(exception)
				.when(jobHandler)
				.execute(job);

		jobWorker.execute(1L);

		verify(jobCompletionService)
				.handleFailure(1L, exception);

		verify(jobCompletionService, never())
				.markCompleted(anyLong());
	}

	@Test
	void shouldUseHandlerForJobType() {

		Job job = createJob(1L);

		when(jobClaimService.claim(1L))
				.thenReturn(job);

		when(jobHandlerRegistry.getHandler(job.getType()))
				.thenReturn(jobHandler);

		jobWorker.execute(1L);

		verify(jobHandlerRegistry)
				.getHandler(job.getType());
	}

	private Job createJob(Long id) {

		Job job = new Job(
				"test-job",
				JobType.SEND_EMAIL,
				Instant.now().plusSeconds(60),
				3
		);

		job.setId(id);

		return job;
	}

	@Test
	void shouldHandleFailureWhenNoHandlerExists() {

		Job job = createJob(1L);

		IllegalStateException exception =
				new IllegalStateException("No handler registered");

		when(jobClaimService.claim(1L))
				.thenReturn(job);

		when(jobHandlerRegistry.getHandler(job.getType()))
				.thenThrow(exception);

		jobWorker.execute(1L);

		verify(jobCompletionService)
				.handleFailure(1L, exception);

		verify(jobCompletionService, never())
				.markCompleted(anyLong());
	}
}