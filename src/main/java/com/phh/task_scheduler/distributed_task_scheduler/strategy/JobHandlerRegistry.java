package com.phh.task_scheduler.distributed_task_scheduler.strategy;

import com.phh.task_scheduler.distributed_task_scheduler.entity.JobType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobHandlerRegistry {

	private final Map<JobType, JobHandler> handlers;

	public JobHandlerRegistry(List<JobHandler> handlers) {

		this.handlers = handlers.stream()
				.collect(Collectors.toMap(
						JobHandler::getType,
						Function.identity()
				));
	}

	public JobHandler getHandler(JobType type) {

		JobHandler handler = handlers.get(type);

		if (handler == null) {
			throw new IllegalStateException(
					"No handler registered for job type: " + type
			);
		}

		return handler;
	}
}