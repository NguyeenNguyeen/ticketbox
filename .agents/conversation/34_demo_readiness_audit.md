# Demo Readiness Audit

## 1. Audit Scope
This audit assesses the out-of-the-box experience for a new developer cloning the TicketBox repository, specifically focusing on the email delivery subsystem. The goal is to determine if a teammate or lecturer can test the email flow immediately without encountering sandbox limitations.

## 2. Seed User Analysis
The application initializes with three predefined users via `data.sql`:
1. `customer1` - `customer1@gmail.com`
2. `admin1` - `admin1@ticketbox.vn`
3. `checker1` - `checker1@ticketbox.vn`

## 3. Sandbox Analysis
The system is currently configured to use `onboarding@resend.dev` as the default sender, which is excellent for local development.
**However**, the Resend Sandbox imposes a strict limitation: emails can only be delivered to the exact email address registered to the Resend API key. 
Therefore, if a developer logs in as `customer1` and purchases a ticket, the email delivery **will fail** (`403 Forbidden`) because `customer1@gmail.com` is a dummy address, not the developer's authorized Resend account email.

## 4. Demo Workflow Analysis
If a fresh clone is evaluated:

### Scenario A: Using Seed Accounts (Fails)
1. Developer logs in as `customer1` (`customer1 / password`).
2. Purchases a ticket.
3. Order completes, RabbitMQ processes the message.
4. Resend API rejects the request because `customer1@gmail.com` is unverified.
*Manual Step Required*: The developer must manually edit `data.sql` or run an `UPDATE` query to change the email to their own verified address.

### Scenario B: Creating a New User (Succeeds)
1. Developer registers a new account using their own verified Resend email address.
2. Developer logs in.
3. Purchases a ticket.
4. Order completes, RabbitMQ processes the message.
5. Resend API accepts the request and delivers the E-ticket.
*Manual Step Required*: None, but the developer must intuitively know to register a new account rather than relying on the provided `customer1`.

## 5. Readiness Score
**Score: 85/100**
The underlying architecture is flawless, but the default seed data creates a friction point that will cause silent background errors if the developer assumes the seed accounts are fully functional for email testing.

## 6. Recommended Improvements
**Smallest Possible Fix:**
Update the project `README.md` or `.env.example` with a explicit warning:
*"To test E-ticket email delivery locally, you MUST register a new user account using the exact email address associated with your Resend API key. Emails sent to the default `customer1` account will be blocked by the Resend Sandbox."*

Alternatively, replace `customer1@gmail.com` in `data.sql` with a clear placeholder like `replace_with_your_resend_email@example.com` to force developers to update it, making the failure obvious rather than mysterious.
