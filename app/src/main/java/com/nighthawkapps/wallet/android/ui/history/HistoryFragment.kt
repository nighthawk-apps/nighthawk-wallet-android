package com.nighthawkapps.wallet.android.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentHistoryBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.onClickNavUp
import com.nighthawkapps.wallet.android.ext.pending
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.WalletBalance
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    private val viewModel: HistoryViewModel by activityViewModel()

    private lateinit var transactionAdapter: TransactionAdapter<ConfirmedTransaction>

    override fun inflate(inflater: LayoutInflater): FragmentHistoryBinding =
        FragmentHistoryBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        twig("HistoryFragment.onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        initTransactionUI()
        binding.backButtonHitArea.onClickNavUp()
        lifecycleScope.launch {
            binding.textAddress.text = viewModel.getAddress().toAbbreviatedAddress(10, 10)
        }
    }

    override fun onResume() {
        twig("HistoryFragment.onResume")
        super.onResume()
        viewModel.balance.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
        viewModel.transactions.collectWith(resumedScope) { onTransactionsUpdated(it) }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        if (balance.availableZatoshi < 0) {
            binding.textBalanceAvailable.text = getString(R.string.updating)
            return
        }

        binding.textBalanceAvailable.text = WalletZecFormmatter.toZecStringShort(balance.availableZatoshi)
        val change = balance.pending
        binding.textBalanceDescription.apply {
            goneIf(change <= 0L)
            val changeString = WalletZecFormmatter.toZecStringFull(change)
            val expecting = R.string.home_banner_expecting.toAppString(true)
            val symbol = getString(R.string.symbol)
            text = "($expecting +$changeString $symbol)".toColoredSpan(R.color.text_light, "+$changeString")
        }
    }

    private fun initTransactionUI() {
        twig("HistoryFragment.initTransactionUI")
        transactionAdapter = TransactionAdapter()
        transactionAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.recyclerTransactions.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            adapter = transactionAdapter
        }
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("HistoryFragment.onTransactionsUpdated")
        transactions.size.let { newCount ->
            twig("got a new paged list of transactions of length $newCount")
            binding.groupEmptyViews.goneIf(newCount > 0)
            val preSize = transactionAdapter.itemCount
            transactionAdapter.submitList(transactions)
        }
    }

    private fun scrollToTop() {
        twig("scrolling to the top")
        binding.recyclerTransactions.apply {
            postDelayed(
                {
                    smoothScrollToPosition(0)
                },
                5L
            )
        }
    }

    // TODO: maybe implement this for better fade behavior. Or do an actual scroll behavior instead, yeah do that. Or an item decoration.
    fun onLastItemShown(item: ConfirmedTransaction, position: Int) {
        binding.footerFade.alpha = position.toFloat() / (binding.recyclerTransactions.adapter?.itemCount ?: 1)
    }
}
