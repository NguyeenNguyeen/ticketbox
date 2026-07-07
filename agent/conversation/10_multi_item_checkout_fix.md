# Objective
Fix the logic bug where checking out multiple ticket categories resulted in only one category being paid and processed.

# Decisions Made
- Modified the frontend `CheckoutForm.tsx` to group all cart items into a single array payload instead of looping and making separate `/api/tickets/reserve` requests.
- Updated `TicketController`'s `PurchaseRequest` class in the backend to receive a list of items (`List<PurchaseItem> items`).
- Refactored `TicketPurchaseService` (`reserveTickets`, `purchaseTicket`) to iterate through the item list, lock categories, verify quantities, and calculate total price for the entire order.
- Modified `finalizeOrderSuccess` and `finalizeOrderFailure` in `TicketPurchaseService` to no longer require `categoryId` and `quantity` parameters. Instead, they dynamically load `OrderItem`s from the database using the `orderId`.
- Updated `StaleOrderCleanupJob` and `PaymentSandboxController` to match the new `TicketPurchaseService` signatures.

# Files Modified
- `apps/frontend/src/components/checkout/CheckoutForm.tsx`
- `apps/backend/src/main/java/com/ticketbox/backend/controller/TicketController.java`
- `apps/backend/src/main/java/com/ticketbox/backend/service/TicketPurchaseService.java`
- `apps/backend/src/main/java/com/ticketbox/backend/scheduler/StaleOrderCleanupJob.java`
- `apps/backend/src/main/java/com/ticketbox/backend/controller/PaymentSandboxController.java`
- `apps/backend/src/test/java/com/ticketbox/backend/service/TicketPurchaseServiceTest.java`

# Remaining Work
- Verify the email contents accurately reflect multiple ticket categories (this should work implicitly as the `EmailTaskMessage` takes a list of `ticketIds`, but needs end-to-end testing).

# Open Questions
- None at this time.
