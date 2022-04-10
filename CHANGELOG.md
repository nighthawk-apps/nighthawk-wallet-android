Change Log
==========

Version 1.0.35 *(2022-04-02)*
------------------------------------
- Update checkpoints & dependencies.
- Migrate to PdfBox lib.

Version 1.0.34 *(2022-02-22)*
------------------------------------
- Update checkpoint, dependencies & Zcash SDK.

Version 1.0.33 *(2022-02-02)*
------------------------------------
- Remove affiliate links.
- Update checkpoints & dependencies.

Version 1.0.32 *(2021-12-04)*
------------------------------------
- Fix tx confirmation count.
- Update checkpoints, Zcash SDK & dependencies.

Version 1.0.31 *(2021-11-21)*
------------------------------------
- Add support to swap coins via StealthEx.io
- Upgrade dependencies and PDF export support library.

Version 1.0.30 *(2021-11-08)*
------------------------------------
- Long tap on transaction under Wallet History now copies tx. id to clipboard.
- Fix char count when typing memo.
- Upgrade dependencies.

Version 1.0.29 *(2021-10-27)*
------------------------------------
- Add support to Buy ZEC via MoonPay.
- Auto Shielding fixes.
- Upgrade dependencies.

Version 1.0.28 *(2021-09-19)*
------------------------------------
- Improve Auto-Shielding.
- Decode memo field when scanning URIs.
- Upgrade dependencies.

Version 1.0.27 *(2021-09-05)*
------------------------------------
- Improve Auto-Shielding.
- Fix test-net build variants.
- Upgrade dependencies to target Android 12.

Version 1.0.26 *(2021-08-21)*
------------------------------------
- NEW: Scan QR codes support on ZecPages.com
- NEW: Deep Link integration with Zcash URI for Payments & Memos.
- Add support for ZIP-321 with single output Zcash URI.
- Minor UI fixes & cleanup.
- Upgrade Gradle and Android dependencies.

Version 1.0.25 *(2021-08-10)*
------------------------------------
- Fix error messaging when sending funds.
- Upgrade Zcash SDK & dependencies.

Version 1.0.24 *(2021-08-08)*
------------------------------------
- NEW: Set up Pin code and Face/Touch ID to access the wallet.

Version 1.0.23 *(2021-08-07)*
------------------------------------
- Fix fastlane setup.

Version 1.0.22 *(2021-08-06)*
------------------------------------
- Upgrade SDK & dependencies.
- UI fixes.
- Add fastlane setup.

Version 1.0.21 *(2021-07-25)*
------------------------------------
- New: Export Wallet Seed Words to a Password Protected PDF.
- New: Default to ZcashBlockExplorer.com for tx details.
- Upgrade dependencies.

Version 1.0.20 *(2021-07-12)*
------------------------------------
- New: Added workflow for automatically shielding funds.
- New: Automatically recover from more network failure states.
- New: Link to play store from the build number.
- New: Hide available/total toggle when there are no pending funds.
- New: Updated checkpoints for mainnet and testnet.
- New: Address tabs with t-address support [Credit @herou].
- New: Balance details screen [Credit @herou].
- New: Better balance information around unmined transactions.
- New: Add toggle to show available vs. total funds.
- New: Auto-shielding via balance details screen.
- Fix: Expand tappable area for showing the balance details.
- Fix: Off by one error when calculating confirmations.
- Fix: Do not show time in transaction details for pending transactions.
- Fix: Repaired QR scanning on older devices (below API 24).
- Fix: Several of the most frequent crashes reported in bugsnag.
- Fix: Corrected over-sized icon in history.
- Fix: History no longer displays negative balance during initial sync.
- Fix: Errors that prevented sync from working in some situations.
- Fix: Improved support for smaller screens and older devices. 
- Update ECC & Android dependencies.

Version 1.0.19 *(2021-05-13)*
------------------------------------
- Hotfix: Remove un-used flags during wallet creation. 

Version 1.0.18 *(2021-05-08)*
------------------------------------
- Add the ability to rescan or wipe the wallet for troubleshooting.
- Fix issue when syncing transactions after sending MAX balance out of wallet.
- Update ECC dependencies.

Version 1.0.17 *(2021-03-31)*
------------------------------------
- Switch price endpoint to api.lightwalletd.com

Version 1.0.16 *(2021-03-24)*
------------------------------------
- Better handling around unsatisfied link errors.

Version 1.0.15 *(2021-03-21)*
------------------------------------
- Fix block rescan error.

Version 1.0.14 *(2021-03-17)*
------------------------------------
- Connect to lightwalletd.com service funded by ZOMG.
- Remove Google Services dependency.
- Support QR code scan on ZecPages.

Version 1.0.13 *(2021-01-24)*
------------------------------------
- Fix crash in magicsnakeloader.
- Handle NumberFormatException.
- Add donation address under Settings.

Version 1.0.12 *(2021-01-20)*
------------------------------------
- Fix crash when restoring wallet.

Version 1.0.11 *(2021-01-18)*
------------------------------------
- Add price query via Nighthawk Server Cached Proxy.
- Update dependencies & Zcash-SDK.

Version 1.0.10 *(2021-01-01)*
------------------------------------
- Fix: Use LockBox Server Settings.
- Update dependencies for material and lottie libs.
- Remove donation box.

Version 1.0.9 *(2020-12-20)*
------------------------------------
- New: Upgrade to the latest Zcash SDK.
- New: Implement ZIP-313, reducing the default fee from 10,000 to 1,000 zats.
- New: Adds authentication prior to viewing backup seed words.
- Fix: Repaired the upgrade flow, which could not reorg because of missing birthday height
- Fix: Repaired create wallet flow which was being covered by the loading screen
- Fix: Authentication bugs on older devices that were preventing sends and mishandling cancels.
- Fix: Users can now upgrade from seed-only prior versions without crashing or needing to restore.
- Fix: Improved internal metrics for troubleshooting issues.
- Fix: Correct race condition when launching the app
- Fix: Display loading screen while waiting for app to initialize
- Add translations for Spanish, Italian, Korea, Russian and Chinese

Version 1.0.8 *(2020-11-15)*
------------------------------------
- Enable deshielding ZEC transaction z -> t
- Update dependencies and gradle build setup
- Simplify Send transaction flow
- Fix importing of wallets with birthday heights after 1,000,000 blocks
- Minor UI Niceties

Version 1.0.7 *(2020-08-29)*
------------------------------------
- Switch default lightwalletd server to Nighthawk's own no-Logs, non-US based server
- Theming & copy updates
- Update dependencies
- Fix MaterialButton styling

Version 1.0.6 *(2020-08-24)*
------------------------------------
- Update to latest librustzcash SDK lib & android dependencies
- Fix New Wallet creation
- Fix SideShift affiliate url
- Fix Donate to Nighthawk copy address
- Add Biometric support
- Add shortcut for auto-fill amount for memo
- Improve compatibility with memo reply-to formats
- Support precise birthday heights for faster restore
- Switch to Reply-To standard for memos

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
