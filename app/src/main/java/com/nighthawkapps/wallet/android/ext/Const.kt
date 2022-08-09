package com.nighthawkapps.wallet.android.ext

import com.nighthawkapps.wallet.android.BuildConfig

object Const {

    const val HOST_SERVER: String = "HOST_SERVER"
    const val HOST_PORT: String = "HOST_PORT"

    /**
     * Named objects for Dependency Injection
     */
    object Name {
        /** application data other than cryptographic keys */
        const val APP_PREFS = "const.name.app_prefs"
        const val BEFORE_SYNCHRONIZER = "const.name.before_synchronizer"
        const val SYNCHRONIZER = "const.name.synchronizer"
    }

    /**
     * Constants used for wallet backup.
     */
    object Backup {
        const val SEED = "cash.z.ecc.android.SEED"
        const val SEED_PHRASE = "cash.z.ecc.android.SEED_PHRASE"
        const val HAS_SEED = "cash.z.ecc.android.HAS_SEED"
        const val HAS_SEED_PHRASE = "cash.z.ecc.android.HAS_SEED_PHRASE"
        const val HAS_BACKUP = "cash.z.ecc.android.HAS_BACKUP"

        // Config
        const val VIEWING_KEY = "cash.z.ecc.android.VIEWING_KEY"
        const val PUBLIC_KEY = "cash.z.ecc.android.PUBLIC_KEY"
        const val BIRTHDAY_HEIGHT = "cash.z.ecc.android.BIRTHDAY_HEIGHT"
    }

    /**
     * Constants used for wallet backup.
     */
    object App {
        const val LAST_VERSION = "const.app.LAST_VERSION"
        const val TRIGGERED_SHIELDING = "const.app.TRIGGERED_SHIELDING"
    }

    /**
     * Default values to use application-wide. Ideally, this set of values should remain very short.
     */
    object Default {

        const val FIRST_USE_VIEW_TX = "const.pref.first_use_view_tx"

        object Server {
            const val HOST = BuildConfig.DEFAULT_SERVER_URL
            const val PORT = 9067
            const val CHROME_PACKAGE = "com.android.chrome"
            const val BUY_ZEC_BASE_URL = "${BuildConfig.MOON_PAY_BASE_URL}?apikey=${BuildConfig.MOON_PAY_KEY}"
        }
    }

    /**
    * Constants for setting PIN
    */
    object PIN {
        const val PIN_CODE = "const.pin.code"
        const val IS_BIO_METRIC_OR_FACE_ID_ENABLED = "const.pin.is_biometric_or_face_id"
    }

    object AppConstants {
        const val ZEC_MAX_AMOUNT = 21000000
        const val AMOUNT_QUERY = "amount"
        const val MEMO_QUERY = "memo"
        const val KEY_ZEC_AMOUNT = "const.app_constants.key_zec_amount"
        const val KEY_LOCAL_CURRENCY = "const.app_constants.key_local_currency"
        const val KEY_SYNC_NOTIFICATION = "const.app_constants.key_sync_notification"
        const val WORKER_TAG_SYNC_NOTIFICATION = "const.app_constants.tag_sync_notification"
        const val USE_UNSTOPPABLE_NAME_SERVICE = "const.app_constants.uns"
    }

    object Network {
        const val COIN_METRICS_BASE_URL = "https://api.coingecko.com/api/v3/"
        const val URL_GET_PRICE = "simple/price"
        const val ZCASH_ID = "zcash"
    }
}
