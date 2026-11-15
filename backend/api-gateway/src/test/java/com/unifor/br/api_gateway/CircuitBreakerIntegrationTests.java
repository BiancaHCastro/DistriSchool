package com.unifor.br.api_gateway;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.resilience4j.circuitbreaker.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import com.unifor.br.api_gateway.security.ResilientHttp;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class CircuitBreakerIntegrationTests {
 @Autowired ResilientHttp http;@Autowired CircuitBreakerRegistry registry;
 @Test void opensAfterFiveFailuresAndReturnsControlled503()throws Exception{
  var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{count.incrementAndGet();exchange.sendResponseHeaders(500,-1);exchange.close();});server.start();
  try{var breaker=registry.circuitBreaker("failed-test");assertEquals(5,breaker.getCircuitBreakerConfig().getSlidingWindowSize());assertEquals(50,breaker.getCircuitBreakerConfig().getFailureRateThreshold());assertEquals(10000L,breaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1).longValue());String url="http://127.0.0.1:"+server.getAddress().getPort()+"/";
   for(int i=0;i<6;i++){var error=assertThrows(ResponseStatusException.class,()->http.exchange("failed-test",url,HttpMethod.GET,new HttpEntity<String>((String)null)));assertEquals(503,error.getStatusCode().value());}
   assertEquals(CircuitBreaker.State.OPEN,breaker.getState());assertEquals(5,count.get());
  }finally{server.stop(0);}
 }
 @Test void authorizationFailuresDoNotOpenCircuit()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{exchange.sendResponseHeaders(403,-1);exchange.close();});server.start();try{for(int i=0;i<6;i++)assertEquals(403,http.exchange("forbidden-test","http://127.0.0.1:"+server.getAddress().getPort(),HttpMethod.GET,new HttpEntity<String>((String)null)).getStatusCode().value());assertEquals(CircuitBreaker.State.CLOSED,registry.circuitBreaker("forbidden-test").getState());}finally{server.stop(0);}
 }
}