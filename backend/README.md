# 📦 Backend - Microsserviços DistriSchool

Cada microsserviço deve ser um projeto Spring Boot independente dentro desta pasta.

## Como adicionar um novo serviço:
1. Gere o projeto no [Spring Initializr](https://start.spring.io/).
2. Use **Java 17** e **Maven** (ou Gradle, se acordado pela equipe).
3. Extraia a pasta gerada diretamente aqui dentro.
   Exemplo de estrutura:
   - `/backend/school-core-service`
   - `/backend/notification-service`
4. Lembre-se de configurar a conexão com o Kafka e o Postgres/Mongo no `application.properties` ou `application.yml` apontando para o Docker (`localhost:5432`, `localhost:9092`).