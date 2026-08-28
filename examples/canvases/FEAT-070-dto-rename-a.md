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

- OrderStatusDto

### Files Likely Affected

- `src/main/java/.../OrderStatusDto.java`
- `src/test/java/.../OrderStatusControllerTest.java`

## O - Operations

### T01 - Add lookup

- Status: Complete
- Description: Service finder is in place

## S - Safeguards

- Do not change auth behavior
- Do not change unrelated API endpoints
