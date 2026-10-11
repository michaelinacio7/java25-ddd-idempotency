# Java 25 + DDD + Idempotency

Projeto de referência com Java 25, Spring Boot, Domain-Driven Design e idempotência HTTP persistente.

## Objetivo

Demonstrar como evitar a execução duplicada de uma operação quando o cliente repete uma requisição usando o mesmo cabeçalho `Idempotency-Key`.

## Comportamento esperado

- Mesma chave e mesmo payload: reproduz a resposta original.
- Mesma chave com payload diferente: retorna `409 Conflict`.
- Chave reservada por uma requisição em andamento: retorna conflito controlado.

## Stack

- Java 25
- Spring Boot
- Maven
- Banco de dados relacional para persistir pagamentos e chaves de idempotência

## Exemplo de requisição

```http
POST /payments
Idempotency-Key: payment-123
Content-Type: application/json

{"amount":149.90,"currency":"BRL"}
```

## Arquitetura

```text
interfaces -> application -> domain
                  ^
                  |
            infrastructure
```

## Status

Projeto em desenvolvimento. Os próximos passos são implementar o domínio, os casos de uso, a persistência e os testes automatizados.
