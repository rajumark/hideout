# Changelog

## 1.0.0

- First version: `Hideout(context).hide(text)` replaces personal information with labels like `[PHONE]`;
  `find(text)` returns each item with its type, exact offsets and a score; `contains(text)`.
- 19 kinds: name, phone, email, UPI ID, Aadhaar, PAN, card, bank account, IFSC / SWIFT, address, passport, voter ID,
  driving licence, vehicle number, other government IDs, date of birth, secrets (passwords, OTPs, PINs, CVVs),
  usernames, IP addresses. Filter by type, set the threshold, or pass your own replacement.
- English, Hinglish, Hindi and other Indian languages, plus European languages.
- Long text in overlapping 128-token windows.
- Pure Kotlin inference with no dependencies, int8 weights (8 MB). minSdk 21.
- Hoverfly Community License: free up to 10,000 monthly active devices per product.
