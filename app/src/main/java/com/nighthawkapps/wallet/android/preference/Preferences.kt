package com.nighthawkapps.wallet.android.preference

import com.nighthawkapps.wallet.android.preference.model.BooleanDefaultValue

object Preferences {
    val isAcknowledgedAutoshieldingInformationPrompt =
        BooleanDefaultValue(PreferenceKeys.IS_AUTOSHIELDING_INFO_ACKNOWLEDGED, false)
}
