# Order Saga with Camunda 8 (Java)

Projeto de estudo de Saga por orquestração visual com BPMN 2.0 e workers Spring Boot.

## Fluxo BPMN

Reserva de estoque -> cobrança simulada -> reserva de entrega. O modelo contém um caminho de compensação para estorno e liberação do estoque quando a etapa de entrega dispara o erro BPMN configurado. O modelo está em `src/main/resources/processes/order-saga.bpmn`.

## Requisitos

- JDK 25
- Maven 3.9+
- Camunda 8 self-managed acessível por REST/gRPC
- Camunda Modeler (opcional, para visualizar o BPMN)

## Configuração

Defina `CAMUNDA_GRPC_ADDRESS` e `CAMUNDA_REST_ADDRESS`. A configuração padrão usa endpoints locais sem autenticação, adequada apenas para desenvolvimento local isolado. Para cluster remoto, configure autenticação e TLS e nunca armazene segredos no Git.

## Workers

Os workers em `OrderWorkers` são stubs de sandbox. Não movimentam dinheiro nem integram com estoque/transportadora reais. Cada integração real deverá usar chave de idempotência, timeouts, retries e compensação segura.

## Executar

```bash
mvn test
mvn spring-boot:run
```

O processo BPMN precisa ser publicado no cluster Camunda antes de iniciar instâncias. A implantação automática do BPMN e os testes com Camunda Process Test ainda não estão configurados.

## Limitações atuais

- Não há endpoint REST para iniciar o processo.
- Os workers retornam resultados simulados.
- A conexão padrão não usa autenticação e deve ficar restrita ao ambiente local.
- O caminho de compensação BPMN precisa ser exercitado com um worker que lance explicitamente o erro de negócio esperado.
- O build ainda precisa ser executado no CI.

Referências:
- [Camunda Spring Boot Starter](https://docs.camunda.io/docs/apis-tools/camunda-spring-boot-starter/getting-started/)
- [Camunda 8 APIs](https://docs.camunda.io/docs/apis-tools/working-with-apis-tools/)
