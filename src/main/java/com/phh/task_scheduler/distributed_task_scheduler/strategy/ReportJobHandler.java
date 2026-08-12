package com.phh.task_scheduler.distributed_task_scheduler.strategy;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReportJobHandler implements JobHandler {

	@Override
	public JobType getType() {
		return JobType.GENERATE_REPORT;
	}

	@Override
	public void execute(Job job) {

		log.info(
				"Generating report for job: id={}",
				job.getId()
		);

		// Actual report logic
	}
}