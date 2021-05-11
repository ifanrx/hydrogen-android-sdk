package com.minapp.android.test.ui.record.query

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import com.minapp.android.sdk.database.Record
import com.minapp.android.sdk.database.Table
import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.database.query.Where
import com.minapp.android.sdk.database.query.WhereOperator
import com.minapp.android.sdk.util.PagedList
import com.minapp.android.test.R
import com.minapp.android.test.ext.autoDisposable
import com.minapp.android.test.ext.createPagedList
import com.minapp.android.test.ext.onTextChanged
import com.minapp.android.test.ui.base.BaseFragment
import com.minapp.android.test.ui.base.BasePageKeyedDataSource
import com.minapp.android.test.ui.widget.RecordLayoutModel_
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_record_query.*

class RecordQueryFragment: BaseFragment() {

    private var state = State()
    private val imm by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record_query, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableET.onTextChanged { state = state.copy(table = it) }
        orderBysET.onItemChanged { state = state.copy(orderBys = it) }
        keysET.onItemChanged { state = state.copy(keys = it) }
        expandsET.onItemChanged { state = state.copy(expands = it) }
        keyET.onTextChanged { state = state.copy(condition = state.condition.copy(key = it)) }
        valueET.onContentChanged { _, value -> state = state.copy(condition = state.condition.copy(value = value)) }
        opSpinner.onOperatorChanged { state = state.copy(condition = state.condition.copy(op = it)) }

        searchBtn.setOnClickListener {
            imm.hideSoftInputFromWindow(it.windowToken, 0)

            Single.fromCallable { createPagedList(DataSource(state)) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this)
                .subscribe(Consumer {
                    list.setController(Controller().apply {
                        submitList(it)
                    })
                })

            if (state.table != state.scheme.table) {
                fetchScheme(state.table)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDisposable(this)
                    .subscribe(Consumer {
                        state = state.copy(scheme = State.Scheme(table = state.table, columns = it))
                        listOf(orderBysET, keysET, expandsET).forEach { it.items = state.scheme.columns }
                        keyET.items = state.scheme.columns
                    })
            }
        }
    }

    private fun fetchScheme(table: String) =
        Single.fromCallable {
            Table(table).query(Query().offset(0).limit(1)).objects.firstOrNull() ?: Record() }
            .map { it._getJson().keySet().filter { !it.isNullOrBlank() }.map { it.trim() }.toList() }
}

data class State (
    val table: String = "",
    val orderBys: List<String> = listOf(),
    val keys: List<String> = listOf(),
    val expands: List<String> = listOf(),
    val scheme: Scheme = Scheme(),
    val condition: Condition = Condition()
) {
    data class Scheme (
        val table: String = "",
        val columns: List<String> = listOf()
    )

    data class Condition (
        val key: String = "",
        val op: WhereOperator = WhereOperator.EQ,
        val value: Any = Unit
    )
}

private class DataSource (
    private val state: State
): BasePageKeyedDataSource<Record>() {

    private val table = Table(state.table)

    override fun loadInitial(query: Query): PagedList<Record>? {
        applyQuery(query)
        return table.query(query)
    }

    override fun loadAfter(query: Query): PagedList<Record>? {
        applyQuery(query)
        return table.query(query)
    }

    private fun applyQuery(query: Query) {
        if (state.orderBys.isNotEmpty())
            query.orderBy(state.orderBys)

        if (state.keys.isNotEmpty())
            query.select(state.keys)

        if (state.expands.isNotEmpty())
            query.expand(state.expands)

        if (!state.condition.key.isNullOrBlank())
            query.put(with(state.condition) {
                val where = Where()
                when (op) {
                    WhereOperator.EQ -> where.equalTo(key, value)
                    WhereOperator.NE -> where.notEqualTo(key, value)
                    WhereOperator.LT -> where.lessThan(key, value)
                    WhereOperator.LTE -> where.lessThanOrEqualTo(key, value)
                    WhereOperator.GT -> where.greaterThan(key, value)
                    WhereOperator.GTE -> where.greaterThanOrEqualTo(key, value)
                    WhereOperator.IS_NULL -> where.isNull(key)
                    WhereOperator.EXISTS -> where.exists(key)
                    WhereOperator.HAS_KEY -> if (value is String) { where.hasKey(key, value) } else { where }
                    else -> where
                }
            })
    }
}

class Controller: PagedListEpoxyController<Record>() {
    override fun buildItemModel(currentPosition: Int, item: Record?): EpoxyModel<*> {
        val record = item ?: Record()
        return RecordLayoutModel_()
            .id(record.hashCode())
            .position(currentPosition)
            .record(record)
    }
}