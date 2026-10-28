# Professor Service — DistriSchool

Microsserviço de professores do projeto DistriSchool, seguindo a mesma divisão arquitetural do `aluno-service`.

## Estrutura

- `controller`: endpoints HTTP e respostas da API.
- `model`: entidade JPA `Professor`.
- `repository`: acesso aos dados com Spring Data JPA.
- `service`: regras de negócio e operações do cadastro.

## Endpoints

- `POST /professores` — cadastrar professor.
- `GET /professores` — listar professores.
- `GET /professores/{id}` — consultar por ID.
- `PUT /professores/{id}` — atualizar cadastro.
- `DELETE /professores/{id}` — excluir cadastro.

## Exemplo de JSON

```json
{
  "nome": "Maria da Silva",
  "email": "maria@escola.edu.br",
  "telefone": "85999990000",
  "especialidade": "Matemática"
}
```

## Observação de configuração

A aplicação está preparada para usar a porta 8083. A conexão com o PostgreSQL e a rota no API Gateway ainda precisam ser configuradas conforme o ambiente do projeto, antes da execução integrada.
