package com.service.infrastructure.http.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceRegistryHttpImpl {

	@Autowired
	private ServiceAPIClient serviceAPIClient;
	
	public ServiceModel insertService(String name, String phone, String email) {
        ServiceModelRequest request = new ServiceModelRequest(name, phone, email);

        return serviceAPIClient.serviceRegister(request);
    }
}
