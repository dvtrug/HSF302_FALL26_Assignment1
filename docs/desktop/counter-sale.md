# Counter sale — TODO 43 / issue #55 (integration pending)

**Draft: backend implemented; the counter UI is not yet delivered.** The user requested waiting for member E's SeatMapView (#54), rather than implementing that member's work. This branch must stay draft until the dependency is available and the counter screen is integrated.

## Implemented by B

- Detached `CustomerItem`, `CounterMovieItem`, `ShowtimeItem` and immutable `SaleResult` records. The counter-specific movie choice avoids introducing a competing shared scheduling model.
- Customer lookup by the existing customer service; schedulable movies and upcoming OPEN showtimes through their existing services. All lazy fields are mapped within the facade read transaction.
- `CounterSaleFacade.sell(customerId, showtimeId, seats, paid)` requires an ADMIN session and wraps create + optional confirm in a single write transaction. Paid produces CONFIRMED; unpaid remains PENDING. The existing BookingService owns seat limits, age rules, pessimistic showtime locking, total calculation, tickets and persistence.
- A failed confirmation rolls back both the new booking and its tickets. Canceling a UI task later will not reverse a sale already committed; the eventual controller must refresh authoritative seat data.

## Verified on H2, 09/11/2026

```powershell
mvn test -Dtest=CounterSaleFacadeTest
mvn clean verify -Dspring.profiles.active=test
```

**5 counter facade tests passed**. The branch's clean verification passed **104 tests**, 0 failures, 0 errors, 0 skipped, and jar packaging succeeded. Coverage includes detached choices, paid/unpaid results, age/seat-limit/permission rejection, rollback after failed confirmation, and two concurrent service calls trying to buy the same seat. The H2 concurrency test uses a counter call and the service path used by web, with separate threads/transactions; exactly one succeeds. This is not a claim that manual TC-D16 has passed on SQL Server or on the completed desktop UI.

## Remaining commits after dependency #54

The planned five UI commits (4–8 in the group plan) are intentionally pending:

1. counter-sale.fxml with left selection pane and E's SeatMapView;
2. customer search with 300 ms debounce;
3. cascading Movie → Showtime → SeatMap, clearing old selection and stale task results;
4. selected-seat count/BigDecimal total and sell-button busy binding;
5. confirmation, sale summary/error dialogs, seat-map reload and view lifecycle handling.

Integrate the exact `SeatMapData`, `setData` and `getSelectedSeats` API that E provides; do not duplicate their component. Coordinate ownership of `CounterSaleFacade.seatMap` mapping as described by Guide TODO 42 before changing it. Then test TC-D13, D15 and D16 on the actual web + desktop sharing SQL Server, and coordinate TC-D14 with E. Capture selected seats, total, paid/unpaid outcomes, underage rejection, seat conflict and the no-duplicate-active-tickets query result.

Evidence lives only under ignored `local-evidence/assignment02/B/todo43/`, with attachments uploaded separately to the PR. This draft contains 5 of the 10 planned commits; after the five UI commits it will reach 10. Base it on the utilities enhancement PR until that PR merges, then retarget to develop. Use **Refs #55** while draft and only close the issue after all acceptance criteria are delivered.
