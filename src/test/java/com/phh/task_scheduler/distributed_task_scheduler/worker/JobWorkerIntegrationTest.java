package com.phh.task_scheduler.distributed_task_scheduler.worker;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import com.phh.task_scheduler.distributed_task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JobWorkerIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:17");

	@DynamicPropertySource
	static void configureProperties(
			DynamicPropertyRegistry registry
	) {
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
	private JobWorker jobWorker;

	@Autowired
	private JobRepository jobRepository;

	@BeforeEach
	void cleanDatabase() {
		jobRepository.deleteAll();
	}

	@Test
	void shouldExecuteJobAndMarkItCompleted() {

		Instant executeAt =
				Instant.now().minusSeconds(10);

		Job job = new Job(
				"email-job",
				JobType.SEND_EMAIL,
				executeAt,
				3
		);

		Job savedJob =
				jobRepository.saveAndFlush(job);

		jobWorker.execute(savedJob.getId());

		Job updated =
				jobRepository.findById(savedJob.getId())
						.orElseThrow();

		assertThat(updated.getStatus())
				.isEqualTo(JobStatus.COMPLETED);

		assertThat(updated.getAttempts())
				.isEqualTo(1);

		assertThat(updated.getWorkerId())
				.isNull();

		assertThat(updated.getLeaseUntil())
				.isNull();

		assertThat(updated.getLastError())
				.isNull();
	}
}