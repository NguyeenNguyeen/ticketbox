# Task Document - Interactive Sandbox Payment Flow

## Objective
Implement an interactive, realistic sandbox payment flow for both VNPAY and MoMo payment methods to replace the instant synchronous mock payment. This enables a complete end-to-end checkout and callback user experience in the local development environment.

## Decisions Made
1. **Two-Step Checkout Flow**: Split the checkout into a Reservation phase (releasing a `PAYING` order from backend) and a Sandbox payment phase.
2. **Local Mock Gateway UI**: Design and implement a client-side `/payment/sandbox` route mimicking both VNPAY (NCB Credit Card form + OTP modal) and MoMo (QR Code scan mock + scanning animations) to preserve 100% offline local testing capability without requiring real merchant registration.
3. **Sandbox Completion API**: Exposed a new REST endpoint `POST /api/payments/sandbox/complete` in backend to securely process the payment state (SUCCESS / FAILED) and run corresponding order finalization (success/failure) services.
4. **Idempotency and Inventory preservation**: Kept the cart intact during the sandbox redirection so that users do not lose their selection if they cancel or exit the gateway. Cart/seat selection are cleared only upon successful callback redirection.

## Files Created
1. **Backend Controller**: [PaymentSandboxController.java](file:///c:/Users/Banana/OneDrive/Desktop/ticketbox/apps/backend/src/main/java/com/ticketbox/backend/controller/PaymentSandboxController.java)
2. **Frontend Page**: [page.tsx](file:///c:/Users/Banana/OneDrive/Desktop/ticketbox/apps/frontend/src/app/payment/sandbox/page.tsx)

## Files Modified
1. **Backend Controller**: [TicketController.java](file:///c:/Users/Banana/OneDrive/Desktop/ticketbox/apps/backend/src/main/java/com/ticketbox/backend/controller/TicketController.java)
2. **Frontend Form**: [CheckoutForm.tsx](file:///c:/Users/Banana/OneDrive/Desktop/ticketbox/apps/frontend/src/components/checkout/CheckoutForm.tsx)
3. **Global Styles**: [globals.css](file:///c:/Users/Banana/OneDrive/Desktop/ticketbox/apps/frontend/src/app/globals.css)
4. **Conversation Records**: [progress_tracker.md](file:///c:/Users/Banana/OneDrive/Desktop/ticketbox/.agents/conversation/progress_tracker.md)

## Dependencies
- Spring Boot core transaction service `TicketPurchaseService`
- Next.js Turbopack compiler and standard CSS animations

## Remaining Work
None. The sandbox flow is fully operational and has been verified locally.

## Open Questions
None.
