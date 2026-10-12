# Order Saga with Temporal (Java)

Projeto independente de estudo para implementar uma Saga orquestrada pelo Temporal Java SDK. O exemplo simula a criação de um pedido com reserva de estoque, cobrança e reserva de entrega. Se uma etapa falhar, as compensações são executadas em ordem inversa.

## Fluxo

1. Reservar estoque.
2. Cobrar pagamento.
3. Reservar entrega.
4. Se alguma etapa falhar, compensar as etapas já iniciadas: cancelar entrega, estornar pagamento e liberar estoque, conforme aplicável.

## Tecnologias

- Java 25
- Maven
- Spring Boot para inicialização do Worker
- Temporal Java SDK
- Docker Compose para Temporal local (a configuração completa do servidor será adicionada em seguida)

## Executar

O projeto está em construção. Configure um servidor Temporal local e as variáveis de conexão antes de executar o Worker. Não use integrações reais nem dados de pagamento reais.

## Princípios importantes

- Workflows devem ser determinísticos; chamadas externas ficam em Activities.
- Activities e compensações precisam ser idempotentes.
- Defina timeouts e políticas de retry por Activity.
- Registre compensações antes da execução de cada efeito externo quando uma falha parcial for possível.
- Compensação é uma nova operação de negócio, não um rollback de banco distribuído.

Referências: [Temporal Java SDK](https://docs.temporal.io/develop/java) e [Saga Pattern](https://docs.temporal.io/develop/java).
