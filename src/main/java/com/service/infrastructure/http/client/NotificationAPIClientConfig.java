package com.service.infrastructure.http.client;

import java.time.Duration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class NotificationAPIClientConfig {

	 @Bean
	 @LoadBalanced
	 public RestClient.Builder loadBalacendRestClientBuilder() {
	      return RestClient.builder();
	 }
	 
	 @Bean
	 public ServiceAPIClient courierAPIClient(RestClient.Builder builder) {
	       RestClient restClient = builder.baseUrl("http://service-register")
	               .requestFactory(generateClientRequestFactory())
	               .build();
	       RestClientAdapter adapter = RestClientAdapter.create(restClient);
	       HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();
	       return proxyFactory.createClient(ServiceAPIClient.class);
	 }

	 private ClientHttpRequestFactory generateClientRequestFactory() {
		 SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
	     factory.setConnectTimeout(Duration.ofMillis(10));
	     factory.setReadTimeout(Duration.ofMillis(200));
	     return factory;
	 }
}
