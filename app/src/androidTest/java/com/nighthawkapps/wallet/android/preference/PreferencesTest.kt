package com.nighthawkapps.wallet.android.preference

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.nighthawkapps.wallet.android.preference.model.DefaultValue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Modifier
import kotlin.reflect.full.memberProperties

@RunWith(AndroidJUnit4::class)
class PreferencesTest {
    @SmallTest
    @Test
    @Throws(IllegalAccessException::class)
    fun fields_public_static_and_final() {
        Preferences::class.java.fields.forEach {
            val modifiers = it.modifiers
            assertThat(Modifier.isFinal(modifiers), equalTo(true))
            assertThat(Modifier.isStatic(modifiers), equalTo(true))
            assertThat(Modifier.isPublic(modifiers), equalTo(true))
        }
    }

    // This test is primary to prevent copy-paste errors in preference keys
    @SmallTest
    @Test
    fun key_values_unique() {
        val fieldValueSet = mutableSetOf<String>()

        Preferences::class.memberProperties
            .map { it.getter.call(Preferences) }
            .map { it as DefaultValue<*> }
            .forEach {
                assertThat(
                    "Duplicate key ${it.key}",
                    fieldValueSet.contains(it.key),
                    equalTo(false)
                )

                fieldValueSet.add(it.key)
            }
    }
}
