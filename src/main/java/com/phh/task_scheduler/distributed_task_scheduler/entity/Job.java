package com.phh.task_scheduler.distributed_task_scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobStatus status;

	@Column(nullable = false)
	private Instant executeAt;

	@Column(nullable = false)
	private int attempts = 0;

	@Column(nullable = false)
	private int maxAttempts = 3;

	@Column(nullable = false)
	private Instant nextAttemptAt;

	private Instant leaseUntil;

	private String workerId;

	@Column(length = 2000)
	private String lastError;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Instant updatedAt;

	@Version
	private Long version;

	public Job(
			String name,
			JobType type,
			Instant executeAt,
			Integer maxAttempts
	) {
		this.name = name;
		this.type = type;
		this.executeAt = executeAt;
		this.maxAttempts = maxAttempts;
		this.status = JobStatus.PENDING;
		this.attempts = 0;
		this.nextAttemptAt = executeAt;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}
}