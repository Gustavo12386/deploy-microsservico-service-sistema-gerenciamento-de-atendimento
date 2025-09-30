package com.service.domain.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.service.api.model.EmailInput;
import com.service.api.model.ServiceInput;
import com.service.domain.model.ServiceEntity;
import com.service.domain.repository.ServiceRepository;
import com.service.infrastructure.kafka.ServiceProducer;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ServiceRegistration {
   
	 @Autowired
	 private ServiceRepository serviceRepository;
	 
	 @Autowired
	 private ServiceProducer serviceProducer;
	 
	 public ServiceEntity create(ServiceInput input) {
		   
	        ServiceEntity service = ServiceEntity.createNewService(
	            input.getName(),
	            input.getPhone(),
	            input.getEmail(),
	            input.getData_agendamento(),   
	            input.getHora_agendamento()
	        );
	        ServiceEntity saved = serviceRepository.saveAndFlush(service);
	        
	        EmailInput emailInput = new EmailInput();
	        emailInput.setId(saved.getId());
	        emailInput.setEmailTo(saved.getEmail());
	        emailInput.setSubject("Você recebeu uma nova solicitação de agendamento!");
	        emailInput.setText(saved.getName() + ", solicitou um agendamento de atendimento! para o dia  \n"
	        + saved.getData_agendamento() + " às: " + saved.getHora_agendamento());

	        // Envia para o Kafka
	        serviceProducer.sendEmail(emailInput);

	        return saved;
	}
	 
	 public ServiceEntity update(UUID serviceId, @Valid ServiceInput input) {
		 ServiceEntity service = serviceRepository.findById(serviceId).orElseThrow();
		 service.setPhone(input.getPhone());
		 service.setName(input.getName());
		 service.setEmail(input.getEmail());
		 return serviceRepository.saveAndFlush(service);
	 }
}
