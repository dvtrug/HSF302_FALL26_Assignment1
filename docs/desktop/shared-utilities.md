# Desktop shared utilities — TODO 35

Baseline: utilities from PR #61. This follow-up keeps the existing APIs and adds:

- `AlertUtil`: wrapped, scrollable messages sized within the primary screen; YES/NO confirmations treat closing as NO. Business exceptions retain their message; unexpected details stay in the log.
- `MoneyUtil.format(BigDecimal)`: Vietnamese grouping and `đ`, null becomes `0 đ`. A fresh formatter per call allows concurrent use. `parse(String)` accepts `1.234.567,50 đ`, `-1.200 ₫` or plain `1000`; malformed grouping, scientific notation and blank input are rejected. Parsing preserves BigDecimal precision.
- `FxValidator.markNodes(errors, fields)`: marks controls or containers with `:error` and tooltip, removes old errors when valid. Existing `mark` delegates to this API. Validation still uses Jakarta annotations on the existing form DTOs.
- `FxAsync.forView(root)`: one task group per controller. Call `group.run(work, success, error)` on the FX thread. Work runs on a virtual thread. Detaching the root cancels pending tasks and suppresses callbacks to the old view. Explicit `close()` is idempotent. A direct task cancellation also releases a supplied busy node.

Read UI input before submitting background work. Never mutate controls in `work`. For a button whose disable property is already bound, use a separate busy property in the binding and pass no busy node to `FxAsync.run`. Cancellation is cooperative: it does not guarantee a JDBC operation is interrupted or undo a transaction already committed. Refresh authoritative data when returning to a view.

## Validation performed

JDK 21, Windows, JavaFX 21.0.5:

```powershell
mvn test -Dtest=MoneyUtilTest,DesktopUtilitiesTest,FxValidatorTest,FxAsyncTest
mvn test -Dspring.profiles.active=test
```

Results on 09/11/2026: **11 utility tests passed**; **99 full regression tests passed**, 0 failures, 0 errors, 0 skipped. The controls/task tests start the real JavaFX toolkit and need a graphical session. Facade tests use H2, not the local SQL Server database.

Evidence is saved locally under `local-evidence/assignment02/B/todo35/` and attached to the PR Conversation separately; it is intentionally excluded from Git. Test log artifacts remain under ignored `target/`.
