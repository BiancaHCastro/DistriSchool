package com.unifor.br.distrischool.turma_service.security;
import java.time.Duration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ResilientHttp {
 private final RestTemplate rest;
 private final CircuitBreakerRegistry circuits;
 public ResilientHttp(CircuitBreakerRegistry circuits){
  this.circuits=circuits; var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofMillis(600));factory.setReadTimeout(Duration.ofMillis(1200));
  rest=new RestTemplate(factory);rest.setErrorHandler(new DefaultResponseErrorHandler(){@Override public boolean hasError(org.springframework.http.client.ClientHttpResponse response){return false;}});
 }
 public ResponseEntity<String> exchange(String service,String url,HttpMethod method,HttpEntity<String> entity){
  try { return circuits.circuitBreaker(service).executeSupplier(()->{var response=rest.exchange(url,method,entity,String.class);if(response.getStatusCode().is5xxServerError())throw new ResourceAccessException("Serviço indisponível");return response;}); }
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Serviço temporariamente indisponível. Tente novamente em instantes.");}
 }
}