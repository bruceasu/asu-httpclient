# Plan: Inventory Consistency Refactor
## Objective
Ensure inventory state consistency during order cancellation.
---
## Background
Currently inventory rollback sometimes fails
when order state changes concurrently.
---
## Non Goals
- No database schema change
- No API redesign
---
## Architecture Context
Relevant modules:
httpclient
json
---
## Plan
### Phase 1 — Investigation
1. Trace order cancel flow
2. Identify concurrency issues
3. Document current logic
Deliverable:
analysis document
---
### Phase 2 — Implementation
1. Introduce idempotency guard
2. Refactor rollback logic
3. Add unit tests
Deliverable:
PR with tests
---
### Phase 3 — Validation
1. Run integration tests
2. Simulate concurrent cancellations
3. Review metrics
---
## Risk Analysis
Possible risks:
- double rollback
- inventory mismatch
Mitigation:
- idempotency key
- integration tests
---
## Validation
Run:
npm test
npm run integration-test
npm run lint
---
## Exit Criteria
Inventory rollback guaranteed.
All tests pass.
No API changes.
