package com.phh.task_scheduler.distributed_task_scheduler.controller;

import com.phh.task_scheduler.distributed_task_scheduler.dto.CreateJobRequest;
import com.phh.task_scheduler.distributed_task_scheduler.dto.JobResponse;
import com.phh.task_scheduler.distributed_task_scheduler.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

	private final JobService jobService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public JobResponse createJob(
			@Valid @RequestBody CreateJobRequest request
	) {
		return jobService.createJob(request);
	}

	@GetMapping("/{id}")
	public JobResponse getJob(
			@PathVariable Long id
	) {
		return jobService.getJob(id);
	}
}