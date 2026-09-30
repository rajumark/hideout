# Changelog

## 2.0.0

- Kotlin Multiplatform: Android, JVM desktop, iOS (arm64 device + simulator), macOS arm64,
  JavaScript and WebAssembly, published to Maven Central as `io.github.rajumark:hideout`.
- New constructor `Hideout()`: the model ships inside the library on every platform, so no
  `Context` is needed. `Hideout(context)` still compiles on Android (deprecated).
- Same model and same results as 1.x; parity with the reference (197 vectors) is tested on every target.
- The sample is now a Compose Multiplatform app (Android, desktop, iOS) plus a web page (JS and Wasm).

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
