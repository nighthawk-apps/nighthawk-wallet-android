Change Log
==========

Version 1.0.5 *(2020-08-01)*
------------------------------------
- Revamp Wallet UI, add Zash info link
- Update donation address, add SideShift.ai integration
- Default to ZecWallet server, Thanks @adityapk!
- Upgrade NDK version, Strigify resources and optimize layouts.

Version 1.0.4 *(2020-07-22)*
------------------------------------
- Fix a bug in resolving transaction history

Version 1.0.3 *(2020-07-20)*
------------------------------------
- New Settings screen with the ability to point to a lighthttpd server of user's choice.
- Switch to "Reply-To" from "sent-from" because the former underscores the idea that the given address is not necessarily the address that originated the transaction.
- Update dependencies and secure the lighthttpd setting via EncryptedSharedPreferences
- Add Donation address

Version 1.0.2 *(2020-07-09)*
------------------------------------
- Remove Feedback Module, Crashlytics & Mixpanel libs
- Fix SSL handshake failure
- Fix for bad QR scan & Navigation after sending ZEC.

Version 1.0.1 *(2020-07-01)*
------------------------------------
- Resolved a critical bug where an EU locale or alternative keyboards defaults to , for decimal on the Send screen and the mobile SDK ignores the denominator on the input screen
- Added further instances of Nighthawk branding (h/t @imichaelmiers)
- Fixes the cursor position resetting to 0 when there's a space on either side of the address field (h/t @CrystalPony)
- Updates dependencies

Version 1.0.0 *(2020-06-16)*
------------------------------------
- Repackage to Nighthawk Wallet
- Removed Analytics Reporting
- Changed package naming & logo
- Removed Send Feedback
- Block sending tx to t addr

Version 1.0.0-alpha23 *(2020-02-21)*
------------------------------------
- Fix: reorg improvements, squashing critical bugs that disabled wallets
- New: extend analytics to include taps, screen views, and send flow.
- New: add crash reporting via Crashlytics.
- New: expose user logs and developer logs as files.
- New: improve feature for creating checkpoints.
- New: added DB schemas to the repository for tracking.
- Fix: numerous bug fixes, test fixes and cleanup.
- New: improved error handling and user experience

Version 1.0.0-alpha17 *(2020-02-07)*
------------------------------------
- New: implemented wallet import
- New: display the memo when tapping outbound transactions
- Fix: removed the sad zebra and softened wording for sending z->t
- Fix: removed restriction on smallest sendable ZEC amount
- Fix: removed "fund now"
- New: turned on developer logging to help with troubleshooting
- New: improved wallet details ability to handle small amounts of ZEC
- New: added ability to clear the memo
- Fix: changed "SEND WITHOUT MEMO" to "OMIT MEMO"
- Fix: corrected wording when the address is included in the memo
- New: display the approximate wallet birthday with the backup words
- New: improved crash reporting
- Fix: fixed bug when returning from the background
- New: added logging for failed transactions
- New: added logic to verify setup and offer explanation when the wallet is corrupted
- New: refactored and improved wallet initialization
- New: added ability to contribute 'plugins' to the SDK
- New: added tons more checkpoints to reduce startup/import time
- New: exposed logic to derive addresses directly from seeds
- Fix: fixed several crashes

Version 1.0.0-alpha11 *(2020-01-15)*
------------------------------------
- Initial ECC release

Version 1.0.0-alpha03 *(2019-12-18)*
------------------------------------
- Initial internal wallet team release
