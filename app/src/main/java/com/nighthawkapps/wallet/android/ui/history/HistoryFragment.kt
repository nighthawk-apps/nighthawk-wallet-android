package com.nighthawkapps.wallet.android.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nighthawkapps.wallet.android.databinding.FragmentHistoryBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.onClickNavUp
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.home.HomeViewModel
import com.nighthawkapps.wallet.android.ui.util.VerticalSpaceItemDecoration

class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    private val viewModel: HistoryViewModel by activityViewModel()
    private val homeViewModel: HomeViewModel by activityViewModel()

    private lateinit var transactionAdapter: TransactionAdapter<ConfirmedTransaction>

    override fun inflate(inflater: LayoutInflater): FragmentHistoryBinding =
        FragmentHistoryBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        twig("HistoryFragment.onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        initTransactionUI()
        binding.backButtonHitArea.onClickNavUp()
    }

    override fun onResume() {
        twig("HistoryFragment.onResume")
        super.onResume()
        viewModel.transactions.collectWith(resumedScope) { onTransactionsUpdated(it) }
    }

    private fun initTransactionUI() {
        twig("HistoryFragment.initTransactionUI")
        transactionAdapter = TransactionAdapter()
        transactionAdapter.setZecConversionValueText(homeViewModel.coinMetricsMarketData.value)
        transactionAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.recyclerTransactions.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            adapter = transactionAdapter
            addItemDecoration(VerticalSpaceItemDecoration(VERTICAL_ITEM_SPACE))
        }
    }

    private fun onTransactionsUpdated(transactions: List<ConfirmedTransaction>) {
        twig("HistoryFragment.onTransactionsUpdated")
        transactions.size.let { newCount ->
            twig("got a new paged list of transactions of length $newCount")
            binding.groupEmptyViews.goneIf(newCount > 0)
            // tricky: we handle two types of lists, empty and PagedLists. It's not easy to construct an empty PagedList so the SDK currently returns an emptyList() but that will not cast to a PagedList
            if (newCount == 0) {
                transactionAdapter.submitList(null)
            } else {
                // tricky: for now, explicitly fail (cast exception) if the transactions are not in a PagedList. Otherwise, this would silently fail to show items and be hard to debug if we're ever passed a non-empty list that isn't an instance of PagedList. This awkwardness will go away when we switch to Paging3
                transactionAdapter.submitList(transactions as PagedList<ConfirmedTransaction>)
            }
        }
    }

    companion object {
        const val VERTICAL_ITEM_SPACE = 18
    }
}
