# Nighthawk Wallet
Zcash wallet using the Zcash Android SDK that is maintained by nighthawk apps.

### Download

<a href="https://play.google.com/store/apps/details?id=com.nighthawkapps.wallet.android">
<img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge.png" height="50px"/></a>

### Setup
To run, clone the repo, open it in Android Studio and press play. It should just work.™

[![Build Status](https://app.bitrise.io/app/f41058ec24e805c8/status.svg?token=4RbH2IZITmTCvom22WGQRg&branch=master)](https://app.bitrise.io/app/f41058ec24e805c8) 

#### Requirements
- [The code](https://github.com/nighthawk-apps/nighthawk-wallet-android)
- [Android Studio](https://developer.android.com/studio/index.html) or [adb](https://www.xda-developers.com/what-is-adb/)
- A device or emulator

### Install from Android Studio
1. Download Android studio and setup an emulator
2. `Import` the zcash-android-wallet folder.  
    It will be recognized as an Android project.
3. Press play (once it is done opening and indexing)

### OR Install from the command line
To build from the command line, [setup ADB](https://www.xda-developers.com/install-adb-windows-macos-linux/) and connect your device. Then simply run this and it will both build and install the app:
```bash
cd /path/to/zcash-android-wallet
./gradlew
```

## Disclosure Policy
Do not disclose any bug or vulnerability on public forums, message boards, mailing lists, etc. prior to responsibly disclosing to Nighthawk Wallet and giving sufficient time for the issue to be fixed and deployed. Do not execute on or exploit any vulnerability.

### Reporting a Bug or Vulnerability
When reporting a bug or vulnerability, please provide the following to nighthawkwallet@protonmail.com

A short summary of the potential impact of the issue (if known).
Details explaining how to reproduce the issue or how an exploit may be formed.
Your name (optional). If provided, we will provide credit for disclosure. Otherwise, you will be treated anonymously and your privacy will be respected.
Your email or other means of contacting you.
A PGP key/fingerprint for us to provide encrypted responses to your disclosure. If this is not provided, we cannot guarantee that you will receive a response prior to a fix being made and deployed.

## Encrypting the Disclosure
We highly encourage all disclosures to be encrypted to prevent interception and exploitation by third-parties prior to a fix being developed and deployed.  Please encrypt using the PGP public key with fingerprint: `8c07e1261c5d9330287f4ec35aff0fd018b01972`

## Disclaimers
There are some known areas for improvement:

- This app depends upon related libraries that it uses. There may be bugs.
- This wallet currently only supports transacting between shielded addresses, which makes it incompatible with wallets that do not support sending to shielded addresses. 
- Traffic analysis, like in other cryptocurrency wallets, can leak some privacy of the user.
- The wallet requires a trust in the lighthttps server to display accurate transaction information. 
- This app has been developed and run exclusively on `mainnet` it might not work on `testnet`.  

See the [Wallet App Threat Model](https://zcash.readthedocs.io/en/latest/rtd_pages/wallet_threat_model.html)
for more information about the security and privacy limitations of the wallet.

## Donate to Nighthawk Devs
zs1nhawkewaslscuey9qhnv9e4wpx77sp73kfu0l8wh9vhna7puazvfnutyq5ymg830hn5u2dmr0sf

### License
MIT
