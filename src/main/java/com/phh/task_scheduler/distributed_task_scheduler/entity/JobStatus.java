package com.phh.task_scheduler.distributed_task_scheduler.entity;

public enum JobStatus {
	PENDING,
	RUNNING,
	COMPLETED,
	FAILED,
	RETRYING,
	CANCELLED
}