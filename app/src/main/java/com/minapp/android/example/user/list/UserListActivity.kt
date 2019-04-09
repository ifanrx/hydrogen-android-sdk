package com.minapp.android.example.user.list

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.minapp.android.example.R
import com.minapp.android.example.base.BaseActivity
import com.minapp.android.example.user.list.rv.UserListAdapter
import com.minapp.android.example.util.TextPanelDialogFragment
import kotlinx.android.synthetic.main.activity_db.*

class UserListActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)
        initList()
    }

    fun initList() {
        val viewModel = provideViewModel(UserListViewModel::class.java)
        val adapter = UserListAdapter(viewModel)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        viewModel.apply {
            data.observe(this@UserListActivity, Observer { adapter.submitList(it) })
            userDetail.observe(this@UserListActivity, Observer {
                it?.also { TextPanelDialogFragment.create(it).show(supportFragmentManager, null) }
            })
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.db_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.add -> {

            }

            R.id.edit -> {

            }

            R.id.delete -> {

            }

            R.id.filter -> {

            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}