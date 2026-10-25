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
5. **Ajuste o API Gateway:** Sempre que um novo serviço for criado, adicione sua respectiva rota no arquivo `application.yml` (localizado em `api-gateway/src/main/resources/`) para que as requisições sejam redirecionadas corretamente.

---

## 🚀 Como rodar e testar o projeto

### Pré-requisitos
Para executar este projeto localmente, você precisará ter instalado:
* **IDE de sua preferência** (IntelliJ IDE ou VS Code).
* **Docker Desktop** (garanta que está instalado e em execução).
* **Cliente de Banco de Dados** (DBeaver, pgAdmin ou a extensão de banco de dados do VS Code).

### Passo a Passo de Execução

1. **Inicie o Docker Desktop** e aguarde até que a engine do Docker esteja pronta.
2. Abra a pasta raiz do projeto (onde está localizado o `docker-compose.yml`) no seu terminal e execute o comando abaixo para construir as imagens e subir todos os serviços:
   ```bash
   docker-compose up --build -d
   ```
3. **Conecte-se ao Banco de Dados:** Abra o seu SGBD (ex: DBeaver) e crie uma nova conexão PostgreSQL na porta `5432`. Utilize as credenciais padrão que estão definidas no arquivo `docker-compose.yml` (consulte as variáveis de ambiente `POSTGRES_USER` e `POSTGRES_PASSWORD` no serviço do banco).

---

## Casos de Teste (Exemplos via cURL)

Assim que os serviços estiverem rodando no Docker, você pode utilizar os comandos abaixo no terminal para validar o funcionamento básico das APIs:

**Caso de Teste 1: Autenticação (Login)**
```bash
curl -X POST http://localhost:8081/login \
-H "Content-Type: application/json" \
-d '{"username": "admin", "password": "123"}'
```

**Caso de Teste 2: Cadastro de um Aluno**
```bash
curl -X POST http://localhost:8082/alunos \
-H "Content-Type: application/json" \
-d '{
    "matricula": "2026001",
    "nome": "Maria Silva",
    "dataNascimento": "24/07/2004",
    "endereco": "Rua Exemplo, 123",
    "contato": "maria@email.com"
}'
```

**Caso de Teste 3: Listagem de Alunos**
```bash
curl -X GET http://localhost:8082/alunos
```