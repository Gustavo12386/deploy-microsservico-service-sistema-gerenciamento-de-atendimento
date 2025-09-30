package com.service;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceSystemApplication.class, args);
	}
	
	 @Bean
	 public Function<String, String> uppercase() {
	    return value -> {
	          System.out.println("Recebido no Lambda: " + value);
	          return value.toUpperCase();
	   };
	 }
	 
	 @Bean
	 public Function<String, String> reverse() {
	     return value -> new StringBuilder(value).reverse().toString();
	 }

}
