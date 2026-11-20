# DistriSchool — Gestão Universitária Distribuída

Microsserviços Java 17 / Spring Boot, PostgreSQL, RabbitMQ e frontend React + Vite. O serviço antigo de login foi substituído por `backend/auth-service`.

## Execução local

1. Copie `.env.example` para `.env` e ajuste `JWT_SECRET` (mínimo de 32 bytes) e `SEED_PASSWORD`, se necessário. O Compose fornece valores de desenvolvimento quando essas variáveis estão ausentes.
2. Execute `docker compose up --build -d --remove-orphans`. A opção remove o container do serviço `login` antigo, liberando a porta 8081; os volumes dos bancos são preservados.
3. Em `frontend`, execute `npm ci` e `npm run dev`. Acesse http://localhost:5173. O botão **Entrar** fica no topo à direita.

| Serviço | Porta |
| --- | --- |
| API Gateway | 8080 |
| auth-service | 8081 |
| aluno-service | 8082 |
| professor-service | 8083 |
| attendance-service | 8084 |
| turma-service | 8085 |
| nota-service | 8086 |
| PostgreSQL acadêmico | 5433 |
| RabbitMQ / gerenciamento | 5672 / 15672 |

O banco `auth-db` é separado e acessível apenas pela rede Docker `distrischool-net`. Os serviços acadêmicos preservam o PostgreSQL compartilhado existente. MongoDB permanece no Compose.

Para publicar a aplicação em uma VM Linux, consulte [o guia de deploy](DEPLOY.md).

## Usuários iniciais

| E-mail | Perfil |
| --- | --- |
| admin@universidade.edu | ROLE_ADMIN |
| prof@universidade.edu | ROLE_PROFESSOR |
| aluno@universidade.edu | ROLE_ALUNO |

Senha inicial comum: **Universidade123!**, ou o valor de `SEED_PASSWORD` na primeira inicialização. O seed é idempotente e não altera senhas existentes.

Na **Área Administrativa**, cadastre alunos/professores e vincule seus IDs aos respectivos usuários em **Usuários → ID do cadastro acadêmico**. Não se presume que o ID do usuário seja o ID acadêmico, evitando associar registros preexistentes à pessoa errada. Após alterar o vínculo ou perfil, entre novamente para emitir um novo token. Cadastre turmas com o professor responsável e efetive matrículas ou aprove as solicitações dos alunos.

## Autenticação e autorização

`POST /api/auth/login` recebe `{"email":"admin@universidade.edu","password":"Universidade123!"}` e retorna `{token,user}`. Envie `Authorization: Bearer <token>` nas demais requisições. O frontend usa `VITE_API_URL` ou http://localhost:8080/api.

Tokens HS256 expiram em uma hora e são validados localmente em cada serviço com a mesma `JWT_SECRET`. A queda do auth-service não invalida tokens já emitidos. Alterações de perfil, vínculo ou exclusão de usuário passam a valer integralmente após a expiração dos tokens anteriores; não há consulta central ou revogação remota.

Administradores gerenciam usuários, cadastros, disciplinas, turmas, matrículas, notas e relatórios. Docentes acessam suas turmas/discentes, editam seu perfil e registram notas/frequência somente para alunos matriculados nas próprias turmas. Alunos consultam o próprio cadastro, notas e frequência, consultam o catálogo e solicitam matrícula. O backend aplica essas regras mesmo quando chamado diretamente.

## Tolerância a falhas

Os eventos de conta, matrícula, falta e notas são publicados de forma síncrona na fila durável `notification.sent`. Se o RabbitMQ estiver indisponível, a operação HTTP segue e registra um aviso. A aprovação de matrícula publica após o commit.

Cada login bem-sucedido agenda um evento JSON `user.logged` em executor limitado. Publicação no RabbitMQ usa a fila durável homônima; falhas são registradas sem bloquear o login. A entrega é de melhor esforço quando o broker está indisponível ou a fila local está cheia; não existe outbox persistente.

Chamadas REST do gateway e verificações acadêmicas usam Resilience4j, com circuitos independentes por serviço: janela de 5 chamadas, mínimo de 5 chamadas, falhas de 50% e abertura por 10 segundos. Timeouts: conexão 600 ms e leitura 1200 ms. Falhas de rede/5xx e circuito aberto retornam 503 controlado; 401/403 são preservados e não abrem circuitos. Se a verificação remota de uma turma falhar, o acesso protegido não é liberado.

## Testes

Backend (em cada pasta de serviço, com JDK 17): `./gradlew test bootJar` ou `.\gradlew.bat test bootJar` no Windows.

Os testes backend usam H2 em modo PostgreSQL, migrações Flyway e validação do esquema. Execute `./gradlew cleanTest test` em cada serviço.

Frontend: `cd frontend`, `npm ci`, `npm test` e `npm run build`. Vitest/Testing Library verificam modal, foco, posicionamento declarado em CSS, persistência, sincronização entre abas, áreas por perfil e 401/403/503. jsdom não mede a posição visual real no navegador.

## Conta, situação acadêmica e exportação

- Administradores criam contas em **Usuários → Criar usuário**, definem a senha inicial e podem vincular o cadastro acadêmico. A nova conta já pode entrar. Para trocar a senha ou liberar uma conta legada pendente, abra **Editar** e preencha **Nova senha**.
- Em ambiente local, `docker compose logs -f auth-service` exibe eventos acadêmicos da fila durável `notification.sent` com o perfil `local-notifications`.
- `GET /api/notas/aluno/{alunoId}/situacao` retorna notas e situação por turma de acordo com o vínculo do usuário.
- As exportações estão em `GET /api/notas/relatorio/export` e `GET /api/attendance/relatorio/export`, com `formato=csv|pdf` e filtros acadêmicos opcionais.

Cada serviço relacional mantém seu histórico Flyway. O baseline 0 executa as migrações idempotentes também em bancos com tabelas preexistentes. O Hibernate valida o esquema no startup. Confira o ambiente com `docker compose ps` e `docker compose logs`.
