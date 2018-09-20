package org.sea9.android.secret

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView

import kotlinx.android.synthetic.main.activity_list.*

class ListActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_list)
		setSupportActionBar(toolbar)

		fab.setOnClickListener { view ->
			Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
					.setAction("Action", null).show()
		}

		// TODO Verify the action and get the query
		if (Intent.ACTION_SEARCH == intent.action) {
			intent.getStringExtra(SearchManager.QUERY)?.also { query ->
				Log.i("SecretPad2", query) // doSearchQuery(query)
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_list, menu)

		// Get the SearchView and set the searchable configuration
		val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
		(menu.findItem(R.id.menu_search).actionView as SearchView).apply {
			// Assumes current activity is the searchable activity
			setSearchableInfo(searchManager.getSearchableInfo(componentName))
			setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
		}

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return when (item.itemId) {
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}
}
