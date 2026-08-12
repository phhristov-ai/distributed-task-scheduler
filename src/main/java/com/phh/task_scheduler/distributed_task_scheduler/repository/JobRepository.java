package com.phh.task_scheduler.distributed_task_scheduler.repository;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

	@Query("""
    SELECT j
    FROM Job j
    WHERE j.status IN (:statuses)
      AND j.nextAttemptAt <= :now
""")
	List<Job> findDueJobs(
			@Param("statuses") Collection<JobStatus> statuses,
			@Param("now") Instant now
	);

	@Modifying
	@Transactional
	@Query("""
    UPDATE Job j
    SET j.status = :running,
        j.attempts = j.attempts + 1,
        j.leaseUntil = :leaseUntil,
        j.workerId = :workerId,
        j.updatedAt = :now
    WHERE j.id = :id
      AND j.status IN (:eligibleStatuses)
      AND j.nextAttemptAt <= :now
""")
	int claimJob(
			@Param("id") Long id,
			@Param("running") JobStatus running,
			@Param("eligibleStatuses") Collection<JobStatus> eligibleStatuses,
			@Param("leaseUntil") Instant leaseUntil,
			@Param("workerId") String workerId,
			@Param("now") Instant now
	);

	@Query("""
    SELECT j
    FROM Job j
    WHERE j.status = :status
      AND j.leaseUntil <= :now
""")
	List<Job> findExpiredJobs(
			@Param("status") JobStatus status,
			@Param("now") Instant now
	);
}