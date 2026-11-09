## Notas sobre a estrutura:

Backend: Dividido em microsserviços (ex: `school-core-service` para gestão de alunos/turmas, notification-service para envio de mensagens). Cada serviço usa Spring Boot com Spring Data JPA e Spring Kafka.

Kafka: Tópicos como student.created, notification.sent são usados para comunicação assíncrona entre serviços.

Frontend: React com Vite para build rápida e desenvolvimento eficiente. Estrutura baseada em componentes e páginas.

Banco de Dados: PostgreSQL para dados relacionais, com Flyway para migrações.

Docker: Cada serviço (backend, frontend, Kafka, DB) roda em containers.

## Funcionalidades e Requisitos

As funcionalidades seguem os mesmos módulos da gestão escolar mencionada

anteriormente, mas com adaptações para Spring Boot, Kafka e React + Vite. Abaixo, destaco como as tecnologias impactam cada módulo e listo os requisitos

funcionais e não funcionais.

| Modulo | Funcionalidades | Requisitos | Requisitos | Integração com |
| --- | --- | --- | --- | --- |
|   | Principais | Funcionais | Não | Tecnologias |
|   |   |   | Funcionais |   |
| Autentica | Cadastro/login de | - Login via | - Resposta | - Spring Boot: Spring |
| ção e | usuários (alunos, | email/senha ou | <2s para | Security com |
| Autorizaç | professores, admins, | OAuth2 (Spring | login. | OAuth2/JWT. |
| ão | pais). | Security). |   |   |
|   |   | - JWT para | - | -Kafka: Publica |
|   | - Recuperação de | autenticação. | Criptografi | eventos de login |
|   | senha. |   | a de | (`user.logged`) para |
|   |   | - Verificação de | senhas | auditoria. |
|   | - Perfis de acesso | email via link. | (BCrypt). |   |
|   | (admin, teacher, |   |   | - React/Vite: |
|   | student, parent). |   | - Suporte a | Formulários de login |
|   |   |   | PT-BR e | responsivos com |
|   |   |   | EN. | validação client- |
|   |   |   |   | side. |
| Gestão | Cadastro/edição/e | - Campos: | - LGPD- | - Spring Boot: |
| de | xclusão de alunos. | nome, data | complian | REST APIs com |
| Alunos |   | nascimento, | t | Spring Data JPA. |


|   | turma. | - Busca por - Upload de documentos. | matrícula, nome, |   | listas |   | endereço, contato. - Exportação de |   |   |   | - |   | (criptogr afia de dados). |   | - Kafka: Evento `student.created ` para notificar outros serviços. |   |   |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|   |   |   |   |   |   |   | (PDF/CSV). |   |   |   |   |   | Interface |   |   |   |   |
|   |   |   |   |   |   |   | - Histórico acadêmico |   |   |   |   |   | responsi va. |   | - React/Vite: Tabelas interativas (ex: |   |   |
|   |   |   |   |   |   |   | integrado. |   |   |   |   |   | - Suporta |   | Material-UI |   |   |
| Gestão de Professo res | horários. | de professores. - Atribuição de | - Cadastro/edição disciplinas/turmas. - Visualização de |   |   | nome, | - Campos: qualificação, contato. - Relatórios de desempenho. - Notificações de atribuições. |   |   |   | . - . |   | >10.000 alunos. - Logs de auditoria Integraçã o com Google Calendar - 99% uptime. | para | DataGrid). - Spring Boot: JPA persistência, Spring Scheduler para lembretes. - Kafka: Evento `teacher.assigne d` para notificações. - React/Vite: Calendário visual |   |   |
| Gestão de Turmas |   | Criação de - Atribuição de | turmas/disciplinas. |   |   | turnos. | | - Suporte a múltiplos |   |   |   | - |   | WebSoc kets para | (ex: APIs. | FullCalendar). - Spring Boot: Spring Web para |   |   |
| e Horários Registro de Presenç as | escolar. faltas. | - Calendário - Relatórios de | alunos/professores - Chamada diária. - Notificações para |   | Excel. - | atraso. | - Detecção de conflitos. - Importação via Presença/falta/ |   |   |   | a - - |   | atualizaç ões em tempo real. - Suporte feriados. Escaláve l para múltiplas escolas. Histórico por ano letivo. |   | -Kafka: Evento `schedule.updat ed` para sincronizar serviços. - React/Vite: Componente de calendário interativo. - Spring Boot: API REST para chamadas. - Kafka: Evento |   |   |
|   | pais. |   |   |   |   |   |   |   |   |   |   |   |   |   | `attendance.rec |   |   |


| - Suporte a | - | orded` para |
| --- | --- | --- |
| biometria/QR | Interface | notificações |
| code. | mobile- | - React/Vite: |
| - Percentual de | friendly. | Formulário |
| presença. | - Backup | simplificado para |
|   | diário. | professores. |
| Gestão |   |   |
| de |   |   |
| Notas e |   |   |
| Avaliaçõ |   |   |
| es |   |   |
