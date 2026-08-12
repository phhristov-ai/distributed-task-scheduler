package com.phh.task_scheduler.distributed_task_scheduler.service;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import com.phh.task_scheduler.distributed_task_scheduler.worker.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {

	private final JobRepository jobRepository;
	private final TaskExecutor jobTaskExecutor;
	private final JobWorker jobWorker;

	public void executeDueJobs() {

		List<Job> jobs = jobRepository.findDueJobs(
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				Instant.now()
		);

		for (Job job : jobs) {
			jobTaskExecutor.execute(
					() -> jobWorker.execute(job.getId())
			);
		}
	}
}