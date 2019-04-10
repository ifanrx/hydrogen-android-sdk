package com.minapp.android.example.content.list

import androidx.paging.DataSource
import com.minapp.android.example.base.BasePageKeyedDataSource
import com.minapp.android.sdk.content.Content
import com.minapp.android.sdk.content.Contents
import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.util.PagedList

class DataSource(
    private val viewModel: ListViewModel
): BasePageKeyedDataSource<Content>() {

    override fun loadInitial(query: Query): PagedList<Content>? {
        query.putAll(viewModel.query)
        return Contents.contents(query)
    }

    override fun loadAfter(query: Query): PagedList<Content>? {
        query.putAll(viewModel.query)
        return Contents.contents(query)
    }

    class Factory(
        private val viewModel: ListViewModel
    ): DataSource.Factory<Int, Content>() {

        override fun create(): DataSource<Int, Content> {
            return DataSource(viewModel)
        }

    }
}