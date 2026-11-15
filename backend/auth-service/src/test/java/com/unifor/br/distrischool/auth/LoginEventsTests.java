package com.unifor.br.distrischool.auth;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoginEventsTests {
 @Test void brokerFailureDoesNotBlockLoginThread()throws Exception{
  var rabbit=mock(RabbitTemplate.class);var called=new CountDownLatch(1);
  doAnswer(invocation->{try{var m=invocation.getArgument(2,Message.class);assertEquals("application/json",m.getMessageProperties().getContentType());assertEquals("user.logged",new ObjectMapper().readTree(m.getBody()).get("event").asText());throw new IllegalStateException("broker down");}finally{called.countDown();}}).when(rabbit).send(eq(""),eq("user.logged"),any(Message.class));
  try(var publisher=new LoginEvents(rabbit)){var u=new UserAccount();u.id=1L;u.email="a@universidade.edu";u.role="ROLE_ADMIN";assertTimeout(Duration.ofMillis(200),()->publisher.publish(u));assertTrue(called.await(2,TimeUnit.SECONDS));}
 }
}