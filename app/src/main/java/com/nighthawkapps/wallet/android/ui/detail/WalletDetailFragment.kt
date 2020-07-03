package com.nighthawkapps.wallet.android.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentDetailBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.onClickNavUp
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Tap.DETAIL_BACK
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class WalletDetailFragment : BaseFragment<FragmentDetailBinding>() {

    override val screen = Report.Screen.DETAIL

    private val viewModel: WalletDetailViewModel by viewModel()

    private lateinit var adapter: TransactionAdapter<ConfirmedTransaction>

    override fun inflate(inflater: LayoutInflater): FragmentDetailBinding =
        FragmentDetailBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButtonHitArea.onClickNavUp { tapped(DETAIL_BACK) }
        lifecycleScope.launch {
            binding.textAddress.text = viewModel.getAddress().toAbbreviatedAddress()
        }
    }

    override fun onResume() {
        super.onResume()
        initTransactionUI()
        viewModel.balance.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        binding.textBalanceAvailable.text = balance.availableZatoshi.convertZatoshiToZecString()
        val change = (balance.totalZatoshi - balance.availableZatoshi)
        binding.textBalanceDescription.apply {
            goneIf(change <= 0L)
            val changeString = change.convertZatoshiToZecString()
            text = "(expecting +$changeString ZEC)".toColoredSpan(R.color.text_light, "+$changeString"
            )
        }
    }

    private fun initTransactionUI() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerTransactions.addItemDecoration(TransactionsFooter(binding.recyclerTransactions.context))
        adapter = TransactionAdapter()
        viewModel.transactions.collectWith(resumedScope) { onTransactionsUpdated(it) }
        binding.recyclerTransactions.adapter = adapter
        binding.recyclerTransactions.smoothScrollToPosition(0)
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        binding.groupEmptyViews.goneIf(transactions.size > 0)
        adapter.submitList(transactions)
    }

    // TODO: maybe implement this for better fade behavior. Or do an actual scroll behavior instead, yeah do that. Or an item decoration.
    fun onLastItemShown(item: ConfirmedTransaction, position: Int) {
        binding.footerFade.alpha =
            position.toFloat() / (binding.recyclerTransactions.adapter?.itemCount ?: 1)
    }
}