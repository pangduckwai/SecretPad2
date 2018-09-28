package org.sea9.android.secret

import android.animation.LayoutTransition
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView

import kotlinx.android.synthetic.main.activity_list.*

class ListActivity : AppCompatActivity(), ContextFragment.Listener, DetailFragment.Listener {
	companion object {
		const val TAG = "secret.main"
	}

	private var ctxFrag: ContextFragment? = null
	private lateinit var searchView: SearchView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Log.d(TAG, "ListActivity.onCreate")
		setContentView(R.layout.activity_list)
		setSupportActionBar(toolbar)

		// this.fab got directly from the layout xml
		fab.setOnClickListener { _ ->
			DetailFragment.getInstance(true, null).show(this.supportFragmentManager, DetailFragment.TAG)
		}

		ctxFrag = supportFragmentManager.findFragmentByTag(ContextFragment.TAG) as ContextFragment?
		if (ctxFrag === null) {
			ctxFrag = ContextFragment()
			supportFragmentManager.beginTransaction().add(ctxFrag as ContextFragment, ContextFragment.TAG).commit()
		}

		handleIntent(intent)
	}

	override fun onNewIntent(intent: Intent) {
		Log.d(TAG, "ListActivity.onNewIntent")
		handleIntent(intent)
	}

	private fun handleIntent(intent: Intent) {
		if (intent.action == Intent.ACTION_SEARCH) {
			intent.getStringExtra(SearchManager.QUERY)?.also { query ->
				doSearch(query)
			}
		}
	}
	private fun doSearch(query: String?) {
		Log.d(TAG, "Searching $query...")
		Snackbar.make(window.decorView, "Searching $query ....", Snackbar.LENGTH_LONG).show() //TODO TEMP
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Log.d(TAG, "ListActivity.onCreateOptionsMenu")

		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_list, menu)

		// Get the SearchView and set the searchable configuration
		val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
		(menu.findItem(R.id.menu_search).actionView as SearchView).apply {
			// Assumes current activity is the searchable activity
			setSearchableInfo(searchManager.getSearchableInfo(componentName))

			searchView = this

			val searchBar = findViewById<ViewGroup>(context.resources.getIdentifier("android:id/search_bar", null, null))
			val transit = LayoutTransition()
			transit.setDuration(LayoutTransition.CHANGE_APPEARING, 0)
			searchBar?.layoutTransition = transit

			val btnid = searchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
			searchView.findViewById<View>(btnid).setOnClickListener {
				searchView.setQuery("", false)
				searchView.isIconified = true
			}
		}

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		// return when (item.itemId) {
		//	R.id.action_settings -> true
		//	else -> super.onOptionsItemSelected(item)
		// }
		when (item.itemId) {
			R.id.action_settings -> {
				Snackbar.make(window.decorView, "Changing settings", Snackbar.LENGTH_LONG).show() //TODO TEMP
				return true
			}
			R.id.action_about -> {
				Snackbar.make(window.decorView, "About...", Snackbar.LENGTH_LONG).show() //TODO TEMP
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	/*=======================================================
	 * @see org.sea9.android.secret.ContextFragment.Listener
	 */
	override fun rowSelectionMade() {
		fragment.view?.requestFocus()
		fab.hide()
	}

	override fun rowSelectionCleared() {
		fab.show()
	}
	//=======================================================

	/*=======================================================
	 * @see org.sea9.android.secret.DetailFragment.Listener
	 */
	override fun onAdd() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun onSave(isNew: Boolean, k: String?, c: String?, t: MutableList<Int>?) {
		if (isNew) {
			ctxFrag?.insertData(k, c, t)
		} else {
			ctxFrag?.updateData(k, c, t)
		}
	}
}