package com.minapp.android.test.ui.record.curd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.minapp.android.sdk.Global
import com.minapp.android.sdk.database.Table
import com.minapp.android.test.R
import com.minapp.android.test.ext.autoDisposable
import com.minapp.android.test.ext.onTextChanged
import com.minapp.android.test.ext.toast
import com.minapp.android.test.ui.base.BaseFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_record_curd.*

class RecordCURDFragment: BaseFragment() {

    private var state = State()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record_curd, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableET.onTextChanged { state = state.copy(table = it) }
        idET.onTextChanged { state = state.copy(id = it) }
        propNameET.onTextChanged { state = state.copy(propName = it) }
        propValueET.onTextChanged { state = state.copy(propValue = it) }

        getByIdBtn.setOnClickListener {
            printWithIO { fetchById() }
        }

        setPropBtn.setOnClickListener {
            printWithIO { fetchById().put(state.propName, state.propValue).update(null) }
        }

        delPropBtn.setOnClickListener {
            printWithIO { fetchById().unset(listOf(state.propName)).save() }
        }

        delBtn.setOnClickListener {
            printWithIO { fetchById().delete() }
        }
    }

    private fun fetchById() = Table(state.table).fetchRecord(state.id)

    private fun printWithIO(block: () -> Any?) {
        Observable.fromCallable { block.invoke() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(this)
            .subscribe({ print(it) }, { toast(it.message) })
    }

    private fun print(any: Any?) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            run { blackboardTv.text = if (any != null) Global.gsonPrint().toJson(any) else "" }
        }
    }
}

private data class State(
    val table: String = "",
    val id: String = "",
    val propName: String = "",
    val propValue: String = ""
)

