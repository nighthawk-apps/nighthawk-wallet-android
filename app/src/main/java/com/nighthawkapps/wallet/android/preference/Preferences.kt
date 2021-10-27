package com.nighthawkapps.wallet.android.preference

import com.nighthawkapps.wallet.android.preference.model.BooleanDefaultValue
import com.nighthawkapps.wallet.android.preference.model.LongDefaultValue

object Preferences {
    val isAcknowledgedAutoshieldingInformationPrompt = BooleanDefaultValue(PreferenceKeys.IS_AUTOSHIELDING_INFO_ACKNOWLEDGED, false)
    val lastAutoshieldingEpochMillis = LongDefaultValue(PreferenceKeys.LAST_AUTOSHIELDING_PROMPT_EPOCH_MILLIS, 0)
}
