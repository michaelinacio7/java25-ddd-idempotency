# Production Readiness Checklist

This is a learning/sandbox project. Do not connect real accounts or money until these are independently reviewed and completed.

- [ ] Security architecture and threat model reviewed.
- [ ] Authentication, token lifecycle and MFA policy defined.
- [ ] Authorization tests cover cross-customer access and IDOR.
- [ ] Migrations and rollback/recovery procedures tested.
- [ ] Double-entry invariants enforced and independently reconciled.
- [ ] Concurrent withdrawals/transfers tested against real PostgreSQL.
- [ ] Idempotency race tests prove one financial effect under concurrent retries.
- [ ] Outbox delivery is retryable and consumers are idempotent.
- [ ] Unknown processor outcomes have reconciliation workflow.
- [ ] Audit logs are immutable, access-controlled and contain no secrets.
- [ ] Encryption, secret rotation, rate limits and fraud controls configured.
- [ ] Backups and point-in-time recovery tested.
- [ ] Load, failure-injection and disaster-recovery tests completed.
- [ ] Applicable banking, privacy, payment-network and financial regulations reviewed by qualified professionals.
- [ ] Test credentials cannot be enabled in production.
