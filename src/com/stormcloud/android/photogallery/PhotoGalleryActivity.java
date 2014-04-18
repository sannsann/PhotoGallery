package com.stormcloud.android.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {
	private static final String TAG = "PhotoGalleryActivity";
	
	@Override
	public Fragment createFragment() {
		return new PhotoGalleryFragment();
	}
	
	// If you need the new intent value, make sure to save it someplace.
	// The value you get from getIntent() will have the old intent, not the new one.
	// This is because getIntent() is intended to return the intent that STARRTED this activity, not the most recent intent it received
	@Override
	public void onNewIntent(Intent intent) {
		PhotoGalleryFragment fragment = (PhotoGalleryFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
		
		if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query: " + query);
			
			// Can use Context.getSharedPreferences(String, int) method as well.
			// However, in practice, you will often not care too much abotu the specific instance, just that it
			// is shared across the entire app.
			
			// In that case, it is better to use the PreferenceManager.getDefaultSharedPreferences(Context) method which
			// returns an instance with a default name and private permissions.
			PreferenceManager.getDefaultSharedPreferences(this)
				.edit() // Called to get an instance of SharedPreferences.Editor, the class used to stash values in SharedPreferences. Allows you to group sets of changes together in transactions, much like it is done with FragmentTransaction. Can goup many changes together into a single storage write operation.
				.putString(FlickrFetchr.PREF_SEARCH_QUERY, query)
				.commit(); // Once done making changes, commit on the editor to make them visible to other users of that SharedPreferences file. 	 
		}
		
		fragment.updateItems();
	}

}
