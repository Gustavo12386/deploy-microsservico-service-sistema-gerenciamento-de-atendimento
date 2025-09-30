package com.service.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class ServiceEntity {
	 @Id
	 @EqualsAndHashCode.Include
	 private UUID id;	
	 private String name;	
	 private String phone;	 
	 private String email;
	 private LocalDate data_agendamento;
	 private LocalTime hora_agendamento;
	 	 
	 public static ServiceEntity createNewService(String name, String phone, String email,
			LocalDate data_agendamento, LocalTime hora_agendamento) {
		    ServiceEntity service = new ServiceEntity();
		    service.setId(UUID.randomUUID());
		    service.setName(name);
		    service.setPhone(phone);
		    service.setEmail(email);
		    service.setData_agendamento(data_agendamento);
		    service.setHora_agendamento(hora_agendamento);
		    return service;
		}
	 
	 
}
