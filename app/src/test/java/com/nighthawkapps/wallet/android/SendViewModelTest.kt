package com.nighthawkapps.wallet.android

import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.nighthawkapps.wallet.android.ui.send.SendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy

class SendViewModelTest {

    @Mock
    lateinit var creatingTx: PendingTransaction
    @Mock
    lateinit var createdTx: PendingTransaction
    @Mock
    lateinit var submittedTx: PendingTransaction
    @Mock
    lateinit var minedTx: PendingTransaction

    @Spy
    lateinit var sendViewModel: SendViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(newSingleThreadContext("Main thread"))

        whenever(creatingTx.id).thenReturn(7)
        whenever(creatingTx.submitAttempts).thenReturn(0)

        whenever(createdTx.id).thenReturn(7)
        whenever(createdTx.raw).thenReturn(byteArrayOf(0x1))

        whenever(submittedTx.id).thenReturn(7)
        whenever(submittedTx.raw).thenReturn(byteArrayOf(0x1))
        whenever(submittedTx.submitAttempts).thenReturn(1)

        whenever(minedTx.id).thenReturn(7)
        whenever(minedTx.raw).thenReturn(byteArrayOf(0x1))
        whenever(minedTx.submitAttempts).thenReturn(1)
        whenever(minedTx.minedHeight).thenReturn(500_001)
    }

    @Test
    fun testUpdateMetrics_creating() {
//        doNothing().whenever(sendViewModel).report(any())

        assertEquals(true, creatingTx.isCreating())
        sendViewModel.updateMetrics(creatingTx)

        verify(sendViewModel).report("7.metric.tx.initialized")
        assertEquals(1, sendViewModel.metrics.size)
        verifyZeroInteractions(feedback)
    }

    @Test
    fun testUpdateMetrics_created() {
        assertEquals(false, createdTx.isCreating())
        assertEquals(true, createdTx.isCreated())
        sendViewModel.updateMetrics(creatingTx)
        sendViewModel.updateMetrics(createdTx)
        Thread.sleep(100)
        println(sendViewModel.metrics)

        verify(sendViewModel).report("7.metric.tx.created")
        assertEquals(1, sendViewModel.metrics.size)
    }

    @Test
    fun testUpdateMetrics_submitted() {
        assertEquals(false, submittedTx.isCreating())
        assertEquals(false, submittedTx.isCreated())
        assertEquals(true, submittedTx.isSubmitSuccess())
        sendViewModel.updateMetrics(creatingTx)
        sendViewModel.updateMetrics(createdTx)
        sendViewModel.updateMetrics(submittedTx)
        assertEquals(5, sendViewModel.metrics.size)

        Thread.sleep(100)
        assertEquals(1, sendViewModel.metrics.size)

        verify(feedback).report(sendViewModel.metrics.values.first())
    }

    @Test
    fun testUpdateMetrics_mined() {
        assertEquals(true, minedTx.isMined())
        assertEquals(true, minedTx.isSubmitSuccess())
        sendViewModel.updateMetrics(creatingTx)
        sendViewModel.updateMetrics(createdTx)
        sendViewModel.updateMetrics(submittedTx)
        sendViewModel.updateMetrics(minedTx)
        assertEquals(7, sendViewModel.metrics.size)

        Thread.sleep(100)
        assertEquals(0, sendViewModel.metrics.size)
    }
}
