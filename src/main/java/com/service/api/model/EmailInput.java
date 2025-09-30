package com.service.api.model;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailInput {
	private UUID id;
	private String emailTo;
	private String subject;
	private String text;	
}
