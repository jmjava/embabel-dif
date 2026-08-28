# REASONS Canvas: FEAT-020-webhook-retry - Retry failed webhook deliveries

## Metadata

- Work ID: FEAT-020-webhook-retry
- Readiness: Ready For Coding

## R - Requirements

### Acceptance Criteria

- [ ] Retry failed webhook deliveries
- [ ] Deliveries must be idempotent

### Non-Goals

- Change payment capture

## E - Entities

### Domain Entities

- WebhookDelivery

### Files Likely Affected

- `src/main/java/.../WebhookWorker.java`

## O - Operations

### T01 - Persist delivery attempts

- Status: Complete
- Description: Store outbound webhook rows

### T02 - Emit events

- Status: Complete
- Description: Publish delivery requested events

### T03 - Add retry integration test

- Status: Not Started
- Description: Prove a failed delivery is retried once and stays idempotent

## S - Safeguards

- Do not change payment capture
