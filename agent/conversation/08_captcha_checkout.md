# CAPTCHA Checkout Gate

## Objective
Add a CAPTCHA verification step before the user can proceed to reserve seats and pay, protecting the reservation endpoint from bot abuse.

## Decisions Made
- **No external API keys**: Used a custom math-based CAPTCHA (canvas-rendered arithmetic challenge) to avoid needing reCAPTCHA/hCaptcha API keys.
- **Canvas rendering**: The challenge is drawn on an HTML5 `<canvas>` with bezier noise lines, random dots, and per-character rotation/position jitter to resist simple OCR.
- **3-attempt limit**: After 3 wrong answers the challenge auto-refreshes.
- **Modal-gate pattern**: The "Thanh toán" button now calls `handlePayClick` → opens `CaptchaModal`. Only `onSuccess` triggers the real `handlePay` flow.

## Files Created
- `apps/frontend/src/components/checkout/CaptchaModal.tsx`

## Files Modified
- `apps/frontend/src/components/checkout/CheckoutForm.tsx`
  - Added `useState(false)` for `showCaptcha`
  - New `handlePayClick` intercepts button click and shows CAPTCHA
  - `handlePay` called via `onSuccess` after verification passes

## Remaining Work
- Optionally swap to Google reCAPTCHA v3 (invisible) once an API key is available.

## Open Questions
- None.
