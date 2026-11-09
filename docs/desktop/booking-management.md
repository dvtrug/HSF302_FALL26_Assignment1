# Booking management — TODO 44 / issue #56

Open DesktopLauncher, sign in with an ADMIN account, choose **Đơn đặt vé**. The screen uses `booking.fxml` and a prototype `BookingAdminController`.

## Behavior

- Combine status, booking date range (inclusive end day) and customer name/email/mobile. Changing a filter resets the page; keyword input is debounced 300 ms. Rows are newest first, 15 per page. Empty results and invalid date ranges are shown without crashing.
- `BookingFacade` maps entity relations inside its read transaction. The UI receives `BookingRow`/`BookingDetail` records, including immutable ticket seat lists; no lazy entities escape to the screen.
- Double-click a row to open the detail modal (customer/mobile, movie, room, showtime, seats, total, status, booking date).
- Confirm is enabled only for PENDING; cancel only for PENDING/CONFIRMED. Both are disabled without selection, during an operation, or for CANCELLED. Each write asks for confirmation and requires an ADMIN desktop session in a write transaction. BR-13 is delegated to BookingService.
- All facade work runs through FxAsync; stale search responses are discarded and detached views cancel pending UI work. Refresh reads current shared database data, including bookings created from the web. Cancellation of a UI task does not undo an already committed write.

## Verification

On Windows / JDK 21, 09/11/2026:

```powershell
mvn test -Dtest=BookingFacadeTest,BookingViewTest
mvn verify -Dspring.profiles.active=test
```

**103 tests passed, 0 failures, 0 errors, 0 skipped; jar/repackage succeeded.** BookingFacadeTest checks filters/pagination, mapping outside transaction, immutable seats, permissions and BR-13 including illegal transitions. BookingViewTest loads the actual FXML with H2 data, opens the detail dialog, persists confirm/cancel through the facade, refreshes and verifies button bindings and the PENDING filter. GUI tests run on Windows with a graphical session; portable facade/validation tests also run without JavaFX UI. UI-specific utilities tests are guarded on non-Windows CI.

Evidence captures the rendered JavaFX view and persisted H2 transitions, not a mockup. The tests invoke facade state transitions programmatically; they are not a claim of a separate manual web-to-desktop SQL Server run. Local SQL Server/web interoperability still needs a running local database and web session for final demo. All evidence stays in the ignored `local-evidence/assignment02/B/todo44/` directory and is uploaded to the PR separately.

## Manual checks for demo

1. TC-D17: filter PENDING → choose row → confirm → accept dialog → refresh shows CONFIRMED; cancel it → CANCELLED. Rejecting the confirmation dialog must leave the status unchanged.
2. TC-D18: select CANCELLED → both action buttons disabled.
3. Double-click: all fields/seat codes visible; closing the modal returns to the list.
4. Create a disposable booking from the web on the same local DB → desktop Refresh displays it. Test a reversed date range and a filter with no results.

PR depends on the utilities enhancement branch until that PR is merged; after merge, retarget this PR to develop. Keep the 10 feature commits when merging rather than squash if the group needs the planned contribution history.
