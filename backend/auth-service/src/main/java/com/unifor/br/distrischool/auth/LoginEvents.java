package com.unifor.br.distrischool.auth;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class LoginEvents implements AutoCloseable {
 private static final Logger log=LoggerFactory.getLogger(LoginEvents.class);
 private final RabbitTemplate rabbit;
 private final ExecutorService executor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(256),new ThreadPoolExecutor.AbortPolicy());
 public LoginEvents(RabbitTemplate rabbit) { this.rabbit=rabbit; }
 @Bean Queue loginQueue(){return new Queue("user.logged",true);}
 public void publish(UserAccount user){
  var event=Map.of("event","user.logged","userId",user.id,"email",user.email,"role",user.role,"occurredAt",Instant.now().toString());
  try{executor.execute(()->{try{
   var properties=new MessageProperties();properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
   rabbit.send("", "user.logged",new Message(new ObjectMapper().writeValueAsBytes(event),properties));
  }catch(Exception e){log.warn("Não foi possível publicar user.logged: {}",e.getClass().getSimpleName());}});}
  catch(RejectedExecutionException e){log.warn("Fila local de eventos de login cheia; login preservado");}
 }
 public void close(){executor.shutdownNow();}
}