package com.phh.task_scheduler.distributed_task_scheduler.strategy;

import com.phh.task_scheduler.distributed_task_scheduler.entity.Job;
import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;

public interface JobHandler {

	JobType getType();

	void execute(Job job);
}