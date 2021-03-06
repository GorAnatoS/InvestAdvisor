package com.invest.advisor.ui.portfolio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.invest.advisor.R
import com.invest.advisor.data.db.entity.EnumMarketData
import com.invest.advisor.data.db.userPortfolio.UserPortfolioEntry
import com.invest.advisor.data.network.ConnectivityInterceptorImpl
import com.invest.advisor.data.network.MoexNetworkDataSourceImpl
import com.invest.advisor.data.network.moexResponse.MarketDataResponse
import com.invest.advisor.data.network.moexResponse.MoexApiService
import com.invest.advisor.databinding.FragmentPortfolioBinding
import com.invest.advisor.ui.base.ScopedFragment
import com.invest.advisor.ui.moex.MoexViewModel
import com.invest.advisor.ui.moex.MoexViewModelFactory
import com.invest.advisor.ui.portfolio.portfolioItems.CardItem
import com.invest.advisor.ui.portfolio.portfolioItems.ExpandablePortfolioItem
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.groupiex.plusAssign
import kotlinx.android.synthetic.main.content_fragment_portfolio.*
import kotlinx.android.synthetic.main.content_fragment_portfolio.view.*
import kotlinx.android.synthetic.main.fragment_portfolio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import kotlin.math.roundToInt

const val INSET_TYPE_KEY = "inset_type"
const val INSET = "inset"

class PortfolioFragment : ScopedFragment(), KodeinAware {

    private val groupAdapter = GroupAdapter<GroupieViewHolder>() //TODO get rid of this parameter

    override val kodein by closestKodein()
    private val viewModelFactory: MoexViewModelFactory by instance()

    var portfolioPurchaseSum = 0.0 //total price of my portfolio when it was bought
    var currentPortfolioPrice = 0.0 //current portfolio price
    var changePrice = 0.0 //currentPortfolioPrice - portfolioPurchaseSum
    var changePercent = 0.0  // changePrice в процентах

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        bindingPortfolio =
            DataBindingUtil.inflate(inflater, R.layout.fragment_portfolio, container, false)

        portfolioViewModel =
            ViewModelProvider(this).get(PortfolioViewModel::class.java)

        bindingPortfolio.userPortfolioViewModel = portfolioViewModel
        bindingPortfolio.lifecycleOwner = this

        val newNumOfShares = Observer<Int> {newNum ->
            textView_analize.isClickable = newNum > 0
            textView_analize.isVisible = newNum > 0
        }

        portfolioViewModel.numOfShares.observe(viewLifecycleOwner, newNumOfShares)

        portfolioViewModel.allData.observe(viewLifecycleOwner, Observer {

            databaseList = it.toMutableList()

            portfolioViewModel.numOfShares.value = databaseList.size


        })



        return bindingPortfolio.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        moexViewModel = ViewModelProvider(this, viewModelFactory).get(MoexViewModel::class.java)
        bindUI()
    }

    private fun bindUI() = launch {

        textView_add.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.moexFragment)
        }

        textView_analize.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.analiticsFragment)
        }

        val mIssApiService = MoexApiService(ConnectivityInterceptorImpl(requireContext()))
        val moexNetworkDataSource = MoexNetworkDataSourceImpl(mIssApiService)

        moexNetworkDataSource.downloadedMarketData.observe(viewLifecycleOwner, Observer { it ->
            if (it == null) return@Observer

            marketDataResponse = it

            groupAdapter.clear()

            textView_add.visibility = View.VISIBLE


            calculatePortfolio()

            //expandable list setting
            val updatedList: MutableList<ExpandablePortfolioItem> = ArrayList()
            val headerList = cardItemList.toList().groupBy { it.entryDatabase.secId }

            for (j in headerList.values) {

                var newItem = ExpandablePortfolioItem(
                    UserPortfolioEntry(
                        j[0].entryDatabase.id,
                        j[0].entryDatabase.secId,
                        j[0].entryDatabase.secPrice,
                        j[0].entryDatabase.secQuantity,
                        j[0].entryDatabase.secPurchaseDate
                    ),
                    j[0].entryMarketData,
                    j.size > 1
                )

                for (k in j.subList(1, j.size)) {
                    newItem = ExpandablePortfolioItem(
                        UserPortfolioEntry(
                            k.entryDatabase.id,
                            k.entryDatabase.secId,
                            ((newItem.entryDatabase.secPrice.toDouble() + k.entryDatabase.secPrice.toDouble()) / 2).toString(),
                            newItem.entryDatabase.secQuantity + k.entryDatabase.secQuantity,
                            k.entryDatabase.secPurchaseDate
                        ),
                        k.entryMarketData,
                        newItem.isExpandable
                    )
                }

                updatedList.add(newItem)
            }

            for ((index, expandableItem) in updatedList.withIndex()) {

                expandableItem.clickListener = { expandableItem ->
                    /*Toast.makeText(context, expandableItem.entryDatabase.secId, Toast.LENGTH_LONG)
                        .show()
*/
                    //Get data from detailed fragment


                    groupIndex = index
                    groupShareName = expandableItem.entryDatabase.secId

                    findNavController().navigate(
                        PortfolioFragmentDirections.actionPortfolioFragmentToDetailedPortfolioItem(
                            expandableItem.entryDatabase.secId
                        )
                    )
                }

                groupAdapter += ExpandableGroup(expandableItem).apply {
                    for (item in cardItemList) {
                        if (item.entryDatabase.secId == expandableItem.entryDatabase.secId)
                            add(
                                CardItem(
                                    item.entryDatabase,
                                    item.entryMarketData
                                )
                            )
                    }
                }
            }

            bindingPortfolio.include.items_container.adapter = groupAdapter


            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("key")
                ?.observe(viewLifecycleOwner) { resultDeleted ->
                    if (resultDeleted && groupIndex != -1) {

                        if (groupIndex < groupAdapter.groupCount) {
                            groupAdapter.remove(groupAdapter.getGroupAtAdapterPosition(groupIndex))

                            groupIndex = -1
                        }


                        databaseList.filter { it.secId == groupShareName }
                            .let { listOfItemsToDelete ->

                                for (item in listOfItemsToDelete) {

                                    portfolioViewModel.delete(
                                        item
                                    )

                                    databaseList.remove(item)

                                    portfolioViewModel.numOfShares.value = databaseList.size

                                    calculatePortfolio()

                                }

                            }

                        calculatePortfolio()

                        bindingPortfolio.include.items_container.adapter?.notifyDataSetChanged()

                    }
                }

            app_bar.visibility = View.VISIBLE
            items_container.visibility = View.VISIBLE
            group_loading.visibility = View.GONE
        })

        GlobalScope.launch(Dispatchers.Main) {
            moexNetworkDataSource.fetchMarketData()
        }
    }

    private fun calculatePortfolio() {
        cardItemList = ArrayList()

        currentPortfolioPrice = 0.0
        portfolioPurchaseSum = 0.0

        for (i in databaseList)
            portfolioPurchaseSum += i.secQuantity.toDouble() * i.secPrice.toDouble()

        //first List of stocks



        for (element in marketDataResponse.currentMarketData.data)
            for (entry in databaseList.toList())
                if (entry.secId == element[EnumMarketData.SECID.ordinal]) {
                    val expandableHeaderItem = ExpandablePortfolioItem(
                        entry,
                        element,
                        false
                    )
                    cardItemList.add(expandableHeaderItem)

                    currentPortfolioPrice += entry.secQuantity.toDouble() * element[EnumMarketData.WAPRICE.ordinal].toDouble()
                }

        if (cardItemList.isNotEmpty()) {
            currentPortfolioPrice = (currentPortfolioPrice * 100).roundToInt() / 100.0

            changePrice = currentPortfolioPrice - portfolioPurchaseSum
            changePrice = (changePrice * 100).roundToInt() / 100.0

            changePercent =
                ((currentPortfolioPrice - portfolioPurchaseSum) / portfolioPurchaseSum)
            changePercent = (changePercent * 100.0).roundToInt() / 1.0
        }

        bindingPortfolio.tvPortfolioInfo.text =
            "Цена портфеля $currentPortfolioPrice₽ ${changePrice} (${changePercent}%)"

    }

    companion object {
        private lateinit var portfolioViewModel: PortfolioViewModel
        lateinit var bindingPortfolio: FragmentPortfolioBinding
        private lateinit var moexViewModel: MoexViewModel

        lateinit var marketDataResponse: MarketDataResponse

        var cardItemList: MutableList<ExpandablePortfolioItem> = ArrayList()

        lateinit var databaseList: MutableList<UserPortfolioEntry>
        private var groupIndex: Int = -1
        private var groupShareName: String = ""

        /* //expandable list setting
         val updatedList: MutableList<ExpandablePortfolioItem> = ArrayList()*/
    }
}

// TODO: 9/26/2020 Время отображаеть без :