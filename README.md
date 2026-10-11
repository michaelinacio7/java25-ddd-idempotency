# Java 25 + DDD + Idempotency

Projeto de referência para demonstrar idempotência HTTP persistente com Java 25, Spring Boot, Maven, JDBC e H2.

## O que demonstra

- Agregado de domínio `Payment` com validação de valor positivo.
- Caso de uso `CreatePaymentService`.
- Portas de repositório separadas da infraestrutura.
- Persistência de pagamentos e chaves de idempotência.
- Fingerprint SHA-256 do payload normalizado.
- Repetição da resposta original para a mesma chave e o mesmo payload.
- `409 Conflict` quando uma chave é reutilizada com outro payload ou uma requisição ainda está em andamento.
- Cabeçalho `Idempotency-Replayed` para indicar replay.

## Requisitos

- JDK 25
- Maven 3.9+

## Executar

```bash
mvn spring-boot:run
```

Executar os testes:

```bash
mvn test
```

A aplicação usa H2 em memória por padrão. Os dados são reiniciados quando a aplicação é encerrada.

## Exemplo

```bash
curl -i -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: payment-123' \
  -d '{"amount":149.90,"currency":"BRL"}'
```

Envie a mesma requisição novamente com a chave `payment-123`: a resposta original deve ser devolvida com `Idempotency-Replayed: true`.

Se você reutilizar a chave com outro valor, por exemplo `199.90`, a API retorna `409 Conflict`.

## Arquitetura

```text
interfaces -> application -> domain
                  ^
                  |
            infrastructure
```

- **interfaces**: adaptador HTTP REST.
- **application**: caso de uso e portas de idempotência.
- **domain**: agregado e contrato do repositório de pagamentos.
- **infrastructure**: adaptadores JDBC e schema SQL.

## Observações

Esta é uma implementação didática para estudo. Para produção, avalie política de expiração das chaves, autenticação, limites de tamanho, métricas, tratamento uniforme de erros, banco de dados externo e testes concorrentes. O exemplo reserva a chave e conclui a operação dentro da transação do caso de uso.
