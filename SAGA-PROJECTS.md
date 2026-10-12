# Saga Pattern Labs

Dois projetos didáticos independentes para comparar orquestração de Saga com Temporal e Camunda 8. Ambos usam Java 25 e Maven e simulam o processo de pedido sem chamar serviços externos reais.

## Projetos

| Projeto | Orquestrador | Ideia principal |
|---|---|---|
| [saga-temporal-java](saga-temporal-java/README.md) | Temporal Java SDK | Workflow escrito em Java, Activities duráveis e compensações registradas no código |
| [saga-camunda-java](saga-camunda-java/README.md) | Camunda 8 | Processo BPMN, workers Java e caminho visual de compensação |

## Cenário de negócio

1. Reservar estoque.
2. Cobrar pagamento.
3. Reservar entrega.
4. Se uma etapa posterior falhar, compensar os efeitos anteriores em ordem inversa.

## Princípios compartilhados

- Operações e compensações idempotentes.
- Chave de negócio estável por pedido para deduplicação.
- Timeouts e retries explícitos.
- Nenhuma chamada de rede dentro de transações locais longas.
- Compensação representa uma nova operação de negócio, não um rollback distribuído.
- Métricas, logs e rastreamento precisam permitir seguir a Saga pelo identificador do pedido.

## Estado

Os projetos são protótipos iniciais: os workers são simulados, o deploy local dos orquestradores e os testes de integração ainda precisam ser completados. O workflow GitHub Actions executa Maven verify para ambos, mas o estado só pode ser confirmado após o CI terminar.

Não use esses exemplos para pagamentos ou integrações de produção sem implementação, testes e revisão de segurança.
