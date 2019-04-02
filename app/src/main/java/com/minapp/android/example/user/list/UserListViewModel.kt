package com.minapp.android.example.user.list

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import com.minapp.android.example.Const
import com.minapp.android.example.base.BaseViewModel
import com.minapp.android.example.user.list.datasource.UserDataSourceFactory
import com.minapp.android.sdk.Global
import com.minapp.android.sdk.user.Users
import kotlinx.coroutines.launch

class UserListViewModel: BaseViewModel() {

    val data = LivePagedListBuilder(UserDataSourceFactory(), Const.DATA_SOURCE_CONFIG).build()
    val userDetail = MutableLiveData<String>()

    fun onItemClick(id: String) {
        ioScope.launch {
            loadingDialog.postValue(true)
            try {
                userDetail.postValue(Global.gsonPrint().toJson(Users.use(id)))
            } catch (e: Exception) {
                Log.e(Const.TAG, e.message, e)
                opToast.postValue(false)
            }
            loadingDialog.postValue(false)
        }
    }
}