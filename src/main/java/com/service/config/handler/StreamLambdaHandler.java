package com.service.config.handler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.service.ServiceSystemApplication;

public class StreamLambdaHandler implements RequestStreamHandler {

	  private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

	    static {
	        try {
	            // Inicializa o container Spring Boot
	            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(ServiceSystemApplication.class);
	        } catch (ContainerInitializationException e) {
	            // Erro durante inicialização da aplicação Spring Boot
	            throw new RuntimeException("Falha ao inicializar o container Spring Boot", e);
	        }
	    }

	    @Override
	    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
	            throws IOException {
	        handler.proxyStream(inputStream, outputStream, context);
}
}