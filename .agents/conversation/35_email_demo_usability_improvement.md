# Email Demo Usability Improvement

## 1. Problem Summary
The underlying email delivery architecture (RabbitMQ, Resend Client, PDF Generator) was confirmed to be fully operational. However, a significant usability friction point remained: new developers and demonstrators were attempting to test the email workflow by logging into the pre-seeded `customer1` account. Because the Resend Sandbox strictly prohibits sending emails to unauthorized addresses, these attempts silently failed in the background (`403 Forbidden`). This led to the false impression that the email feature was broken, when in reality, it was just a sandbox limitation.

## 2. Changes Made
This improvement focuses entirely on developer onboarding and documentation. No backend business logic, infrastructure configurations, or architectural structures were altered. The goal was to make the sandbox limitations explicitly clear to any new contributor cloning the project.

## 3. README.md Updates
Appended a new section: **"📧 Hướng Dẫn Test Tính Năng Email (E-ticket)"**
This section explicitly documents:
1. The project utilizes the **Resend Sandbox Mode** (`onboarding@resend.dev`) by default.
2. Sandbox emails can ONLY be sent to the developer's registered Resend email address.
3. A strict warning **NOT** to use the `customer1` seed account for email testing.
4. A clear, 6-step recommended workflow explaining how to properly test the feature by registering a new account using the developer's verified email.

## 4. .env.example Updates
- Overhauled the email configuration block in `.env.example`.
- Removed the confusing `EMAIL_FROM=tickets@ticketbox.vn` property to prevent new developers from accidentally overwriting the local development defaults.
- Replaced it with `EMAIL_FROM=onboarding@resend.dev`.
- Added a prominent, multi-line comment block (in Vietnamese) warning users about the sandbox restrictions and the necessity of creating a new user account.

## 5. Seed Data Updates (Optional Improvement)
- Added an explicit SQL comment directly above the `customer1` insertion block in `data.sql`:
  `-- LƯU Ý: TÀI KHOẢN NÀY KHÔNG DÙNG ĐỂ TEST EMAIL. EMAIL CHỈ HOẠT ĐỘNG VỚI RESEND NẾU BẠN TỰ TẠO TÀI KHOẢN MỚI CÙNG EMAIL RESEND.`
This prevents database developers from assuming the seed accounts are universally functional for all features.

## 6. Validation Results
A new developer reading the `README.md` or `.env.example` is now immediately confronted with clear, unambiguous instructions on how the Resend Sandbox operates. They are explicitly told *why* seed accounts will fail and are provided the exact steps to successfully test the E-ticket workflow, completely eliminating the confusion identified in the previous audit.
