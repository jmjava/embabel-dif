# REASONS Canvas: FEAT-070-dto-rename - Order status lookup

## Metadata

- Work ID: FEAT-070-dto-rename
- Readiness: Ready For Coding

## R - Requirements

### Acceptance Criteria

- [ ] `GET /api/orders?email=` returns matching orders

### Non-Goals

- Pagination

## E - Entities

### Domain Entities

- OrderLookupResponse

### Files Likely Affected

- `src/main/java/.../OrderLookupResponse.java`
- `src/test/java/.../OrderLookupIT.java`

## O - Operations

### T01 - Add lookup

- Status: Complete
- Description: Service finder is in place

## S - Safeguards

- Do not change auth behavior
- Do not change unrelated API endpoints
