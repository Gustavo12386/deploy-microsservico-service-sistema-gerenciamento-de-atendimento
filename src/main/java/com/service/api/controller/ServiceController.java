package com.service.api.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMethod;

import com.service.api.model.ServiceInput;
import com.service.domain.model.ServiceEntity;
import com.service.domain.repository.ServiceRepository;
import com.service.domain.service.ServiceRegistration;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Slf4j
public class ServiceController {
   
	@Autowired
	private ServiceRegistration serviceRegistration;
	
	@Autowired
    private ServiceRepository serviceRepository;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServiceEntity create(@Valid @RequestBody ServiceInput input) {
        return serviceRegistration.create(input);
	}
	
	@PutMapping("/{id}")
    public ServiceEntity update(@PathVariable UUID serviceId,
                          @Valid @RequestBody ServiceInput input) {
        return serviceRegistration.update(serviceId, input);
    }
	
	@GetMapping
	public Page<ServiceEntity> findAll(@PageableDefault Pageable pageable) {
	     return serviceRepository.findAll(pageable);
	}


	@GetMapping("/{id}")
	public ServiceEntity findById(@PathVariable UUID serviceId) {
	    return serviceRepository.findById(serviceId)
	    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
	
	@DeleteMapping("/{id}")
	public void remove(@PathVariable UUID id) {
		serviceRepository.deleteById(id);
	}
	
	
}
