package com.service.infrastructure.http.client;

import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@HttpExchange("/api/v1/services")
public interface ServiceAPIClient {
	@PostExchange("/service-register")
    @Retry(name = "Retry_ServiceAPIClient_emailSend")
    @CircuitBreaker(name = "CircuitBreaker_ServiceAPIClient_serviceRegister")
	ServiceModel serviceRegister(ServiceModelRequest request);
}
