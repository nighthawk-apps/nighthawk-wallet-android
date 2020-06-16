# Nighthawk Wallet
An Android wallet using the Zcash Android SDK that is maintained by nighthawk apps devs.
Our first Nighthawk Wallet release is based upon the Electric Coin Company's shielded reference wallet for Zcash. We've removed analytics reporting and logging, disabled sending to transparent addresses, and updated the branding. Please help us beta test and send feedback to nighthawkwallet@protonmail.com or Keybase (@NighthawkWallet). Be free with z2z!

### Setup

To run, clone the repo, open it in Android Studio and press play. It should just work.™

#### Requirements
- [The code](https://github.com/zcash/zcash-android-wallet)
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

## Disclaimers
There are some known areas for improvement:

- This app is mainly intended for learning and improving the related libraries that it uses. There may be bugs.
- This wallet currently only supports transacting between shielded addresses, which makes it incompatible with wallets that do not support sending to shielded addresses. 
- Traffic analysis, like in other cryptocurrency wallets, can leak some privacy of the user.
- The wallet requires a trust in the server to display accurate transaction information. 
- This app has been developed and run exclusively on `mainnet` it might not work on `testnet`.  

See the [Wallet App Threat Model](https://zcash.readthedocs.io/en/latest/rtd_pages/wallet_threat_model.html)
for more information about the security and privacy limitations of the wallet.

### License
MIT
