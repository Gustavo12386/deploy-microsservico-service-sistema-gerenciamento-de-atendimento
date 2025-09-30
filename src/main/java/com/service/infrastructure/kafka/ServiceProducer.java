package com.service.infrastructure.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.service.api.model.EmailInput;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ServiceProducer {
	 
	 @Autowired
	 private KafkaTemplate<String, Object> kafkaTemplate;
	 
	 private static final String TOPIC = "email-topic";

	 public void sendEmail(EmailInput emailInput) {
	        kafkaTemplate.send(TOPIC, emailInput);
	    }   
}
