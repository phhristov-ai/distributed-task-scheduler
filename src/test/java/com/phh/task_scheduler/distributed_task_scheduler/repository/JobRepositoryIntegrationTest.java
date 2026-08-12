package com.phh.task_scheduler.distributed_task_scheduler.repository;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@Testcontainers
class JobRepositoryIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:17");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add(
				"spring.datasource.url",
				postgres::getJdbcUrl
		);
		registry.add(
				"spring.datasource.username",
				postgres::getUsername
		);
		registry.add(
				"spring.datasource.password",
				postgres::getPassword
		);
	}

	@Autowired
	private JobRepository jobRepository;

	@BeforeEach
	void cleanDatabase() {
		jobRepository.deleteAll();
	}

	@Test
	void shouldFindDuePendingJobs() {

		Job job = new Job(
				"test-job",
				JobType.SEND_EMAIL,
				Instant.now().minusSeconds(10),
				3
		);

		jobRepository.saveAndFlush(job);

		List<Job> jobs = jobRepository.findDueJobs(
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				Instant.now()
		);

		assertThat(jobs)
				.hasSize(1)
				.extracting(Job::getName)
				.containsExactly("test-job");
	}

	@Test
	void shouldFindDuePendingAndRetryingJobs() {

		Instant now = Instant.now();

		Job pendingJob = new Job(
				"pending-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(10),
				3
		);

		Job retryingJob = new Job(
				"retrying-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(20),
				3
		);

		retryingJob.setStatus(JobStatus.RETRYING);
		retryingJob.setNextAttemptAt(now.minusSeconds(5));

		Job futureJob = new Job(
				"future-job",
				JobType.SEND_EMAIL,
				now.plusSeconds(60),
				3
		);

		jobRepository.saveAllAndFlush(
				List.of(
						pendingJob,
						retryingJob,
						futureJob
				)
		);

		List<Job> jobs = jobRepository.findDueJobs(
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				now
		);

		assertThat(jobs)
				.extracting(Job::getName)
				.containsExactlyInAnyOrder(
						"pending-job",
						"retrying-job"
				);
	}

	@Test
	void shouldClaimPendingJob() {

		Instant now = Instant.now();
		Instant leaseUntil = now.plusSeconds(30);

		Job job = new Job(
				"test-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(10),
				3
		);

		jobRepository.saveAndFlush(job);

		int claimed = jobRepository.claimJob(
				job.getId(),
				JobStatus.RUNNING,
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				leaseUntil,
				"worker-1",
				now
		);

		assertThat(claimed).isEqualTo(1);

		Job updated = jobRepository.findById(job.getId())
				.orElseThrow();

		assertThat(updated.getStatus())
				.isEqualTo(JobStatus.RUNNING);

		assertThat(updated.getAttempts())
				.isEqualTo(1);

		assertThat(updated.getWorkerId())
				.isEqualTo("worker-1");

		assertThat(updated.getLeaseUntil())
				.isCloseTo(leaseUntil, within(1, ChronoUnit.MICROS));
	}

	@Test
	void shouldNotClaimAlreadyRunningJob() {

		Instant now = Instant.now();

		Job job = new Job(
				"test-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(10),
				3
		);

		jobRepository.saveAndFlush(job);

		int firstClaim = jobRepository.claimJob(
				job.getId(),
				JobStatus.RUNNING,
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				now.plusSeconds(30),
				"worker-1",
				now
		);

		int secondClaim = jobRepository.claimJob(
				job.getId(),
				JobStatus.RUNNING,
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				now.plusSeconds(30),
				"worker-2",
				now
		);

		assertThat(firstClaim).isEqualTo(1);
		assertThat(secondClaim).isEqualTo(0);

		Job updated = jobRepository.findById(job.getId())
				.orElseThrow();

		assertThat(updated.getAttempts())
				.isEqualTo(1);

		assertThat(updated.getWorkerId())
				.isEqualTo("worker-1");
	}

	@Test
	void shouldFindExpiredRunningJobs() {

		Instant now = Instant.now();

		Job expiredJob = new Job(
				"expired-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(60),
				3
		);

		expiredJob.setStatus(JobStatus.RUNNING);
		expiredJob.setLeaseUntil(now.minusSeconds(10));
		expiredJob.setWorkerId("worker-1");

		Job activeJob = new Job(
				"active-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(60),
				3
		);

		activeJob.setStatus(JobStatus.RUNNING);
		activeJob.setLeaseUntil(now.plusSeconds(30));
		activeJob.setWorkerId("worker-2");

		jobRepository.saveAllAndFlush(
				List.of(expiredJob, activeJob)
		);

		List<Job> jobs = jobRepository.findExpiredJobs(
				JobStatus.RUNNING,
				now
		);

		assertThat(jobs)
				.extracting(Job::getName)
				.containsExactly("expired-job");
	}

	@Test
	void shouldOnlyFindExpiredRunningJobs() {

		Instant now = Instant.now();

		Job pending = new Job(
				"pending",
				JobType.SEND_EMAIL,
				now.minusSeconds(60),
				3
		);

		pending.setLeaseUntil(now.minusSeconds(10));

		Job completed = new Job(
				"completed",
				JobType.SEND_EMAIL,
				now.minusSeconds(60),
				3
		);

		completed.setStatus(JobStatus.COMPLETED);
		completed.setLeaseUntil(now.minusSeconds(10));

		Job expiredRunning = new Job(
				"expired-running",
				JobType.SEND_EMAIL,
				now.minusSeconds(60),
				3
		);

		expiredRunning.setStatus(JobStatus.RUNNING);
		expiredRunning.setLeaseUntil(now.minusSeconds(10));

		jobRepository.saveAllAndFlush(
				List.of(
						pending,
						completed,
						expiredRunning
				)
		);

		List<Job> jobs = jobRepository.findExpiredJobs(
				JobStatus.RUNNING,
				now
		);

		assertThat(jobs)
				.extracting(Job::getName)
				.containsExactly("expired-running");
	}

	@Test
	void shouldFindRetryingJobWhenNextAttemptHasArrived() {

		Instant now = Instant.now();

		Job job = new Job(
				"retry-job",
				JobType.SEND_EMAIL,
				now.plusSeconds(60),
				3
		);

		job.setStatus(JobStatus.RETRYING);
		job.setNextAttemptAt(now.minusSeconds(1));
		job.setAttempts(1);

		jobRepository.saveAndFlush(job);

		List<Job> jobs = jobRepository.findDueJobs(
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				now
		);

		assertThat(jobs)
				.extracting(Job::getName)
				.containsExactly("retry-job");
	}

	@Test
	void shouldNotFindRetryingJobBeforeNextAttempt() {

		Instant now = Instant.now();

		Job job = new Job(
				"future-retry",
				JobType.SEND_EMAIL,
				now,
				3
		);

		job.setStatus(JobStatus.RETRYING);
		job.setAttempts(1);
		job.setNextAttemptAt(now.plusSeconds(60));

		jobRepository.saveAndFlush(job);

		List<Job> jobs = jobRepository.findDueJobs(
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				now
		);

		assertThat(jobs).isEmpty();
	}

	@Test
	void shouldClaimRetryingJob() {

		Instant now = Instant.now();

		Job job = new Job(
				"retry-job",
				JobType.SEND_EMAIL,
				now,
				3
		);

		job.setStatus(JobStatus.RETRYING);
		job.setAttempts(1);
		job.setNextAttemptAt(now.minusSeconds(1));

		jobRepository.saveAndFlush(job);

		int claimed = jobRepository.claimJob(
				job.getId(),
				JobStatus.RUNNING,
				List.of(
						JobStatus.PENDING,
						JobStatus.RETRYING
				),
				now.plusSeconds(30),
				"worker-1",
				now
		);

		assertThat(claimed).isEqualTo(1);

		Job updated = jobRepository.findById(job.getId())
				.orElseThrow();

		assertThat(updated.getStatus())
				.isEqualTo(JobStatus.RUNNING);

		assertThat(updated.getAttempts())
				.isEqualTo(2);

		assertThat(updated.getWorkerId())
				.isEqualTo("worker-1");
	}

	@Test
	void shouldAllowOnlyOneWorkerToClaimJobConcurrently()
			throws Exception {

		Instant now = Instant.now();

		Job job = new Job(
				"concurrent-job",
				JobType.SEND_EMAIL,
				now.minusSeconds(10),
				3
		);

		jobRepository.saveAndFlush(job);

		int numberOfWorkers = 2;

		ExecutorService executor =
				Executors.newFixedThreadPool(numberOfWorkers);

		CyclicBarrier barrier =
				new CyclicBarrier(numberOfWorkers);

		Callable<Integer> claimTask = () -> {

			barrier.await();

			return jobRepository.claimJob(
					job.getId(),
					JobStatus.RUNNING,
					List.of(
							JobStatus.PENDING,
							JobStatus.RETRYING
					),
					now.plusSeconds(30),
					UUID.randomUUID().toString(),
					now
			);
		};

		try {

			List<Future<Integer>> results =
					executor.invokeAll(
							List.of(
									claimTask,
									claimTask
							)
					);

			int firstResult = results.get(0).get();
			int secondResult = results.get(1).get();

			assertThat(firstResult + secondResult)
					.isEqualTo(1);

			Job updated = jobRepository.findById(job.getId())
					.orElseThrow();

			assertThat(updated.getStatus())
					.isEqualTo(JobStatus.RUNNING);

			assertThat(updated.getAttempts())
					.isEqualTo(1);

			assertThat(updated.getWorkerId())
					.isNotNull();

			assertThat(updated.getLeaseUntil())
					.isNotNull();

		} finally {
			executor.shutdown();
		}
	}
}