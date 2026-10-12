# Order Saga with Camunda 8 (Java)

Projeto independente de estudo para orquestrar o mesmo fluxo de pedido usando Camunda 8 e BPMN. O processo modela reserva de estoque, cobrança, entrega e caminhos de compensação em caso de falha.

## Fluxo

1. Reservar estoque.
2. Cobrar pagamento.
3. Reservar entrega.
4. Se uma etapa falhar, executar as compensações necessárias em ordem inversa e terminar com estado de falha compensada.

## Tecnologias

- Java 25
- Spring Boot 3.5
- Camunda 8 Spring Boot Starter
- BPMN 2.0
- Maven

## Executar

Este projeto precisa de um cluster Camunda 8 local ou remoto. Configure endpoint e autenticação em variáveis de ambiente; não comite tokens ou credenciais. O BPMN é mantido em `src/main/resources/processes`.

## Conceitos demonstrados

- Orquestração visual com BPMN.
- Service Tasks processadas por workers Java.
- Boundary/error events para caminhos de falha.
- Compensações como tarefas explícitas e idempotentes.
- Retries, incidentes e observabilidade do processo no Camunda Operate.

Referências: [Camunda Spring Boot Starter](https://docs.camunda.io/docs/apis-tools/camunda-spring-boot-starter/getting-started/) e [Camunda 8 APIs](https://docs.camunda.io/docs/apis-tools/working-with-apis-tools/).
