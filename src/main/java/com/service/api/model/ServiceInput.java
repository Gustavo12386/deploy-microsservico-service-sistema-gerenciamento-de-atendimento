package com.service.api.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceInput {
    @NotBlank
	private String name;
	@NotBlank
	private String phone;
	@NotBlank
	private String email;
	@NotNull
	private LocalDate data_agendamento;
	@NotNull
	private LocalTime hora_agendamento;
}
