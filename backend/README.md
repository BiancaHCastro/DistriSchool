# Backend universitário

Consulte o [README principal](../README.md) para execução, credenciais, vínculos acadêmicos, regras de acesso e testes.

Os sete serviços usam Gradle e Java 17. O auth-service substitui o antigo login. Configure a mesma variável JWT_SECRET em todos os processos. Para execução fora do Compose, ajuste também as URLs de PostgreSQL, RabbitMQ e dos serviços REST; os defaults apontam para os nomes DNS da rede Docker.

As rotas são encaminhadas por api-gateway/controller/GatewayProxyController.java. A mensageria existente foi preservada: student.created, teacher.assigned, attendance.recorded, schedule.updated e fila.calculo.media. O auth-service acrescenta user.logged em JSON.
