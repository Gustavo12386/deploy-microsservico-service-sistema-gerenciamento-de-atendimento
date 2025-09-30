package com.service.infrastructure.http.client;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class BadGatewayException extends RuntimeException {

	public BadGatewayException() {
    }

    public BadGatewayException(Throwable cause) {
        super(cause);
    }
}
