package com.stormcloud.android.photogallery;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

public class PhotoGalleryFragment extends VisibleFragment {
	private static final String TAG = "PhotoGalleryFragment";
	
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		//new FetchItemsTask().execute();
		updateItems();
		
//		Intent i = new Intent(getActivity(), PollService.class);
//		getActivity().startService(i);
		
		//PollService.setServiceAlarm(getActivity(), true);
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
			public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
				// Ensure that image is not being set on a stale ImageView
				if (isVisible()) {
					imageView.setImageBitmap(thumbnail);
				}
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper(); // .start() is called beore getLooper() to ensure that thread's guts are ready before preceeding.x	
		Log.i(TAG, "Background thread started");
	}
	
	public void updateItems() {
		new FetchItemsTask().execute();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView)v.findViewById(R.id.gridView);
		
		setupAdapter();
		
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> gridView, View view, int pos, long id) {
				GalleryItem item = mItems.get(pos);
				
				Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
				//Intent i = new Intent(Intent.ACTION_VIEW, photoPageUri);
				Intent i = new Intent(getActivity(), PhotoPageActivity.class);
				
				i.setData(photoPageUri);
				startActivity(i);
			}
		});
		
		return v;
	}
	
	// Old header updated in favor of below.
	// private class FetchItemsTask extends AsyncTask<Void, Void, Void> {
	
	// First parameter in the list allows specification of the type of input parameters
	//	Input parameters are passed in to the execute(....) method, which takes in a variable number of arguments.
	//	These variable arguments are then passe on to doInBakground(...)
	
	// Second parameters allows to specify the type for sending progress updates.
	// Progress updates usually happen in the middle of an ongoing background process.
	// The problem is that you cannot make necessary UI updates insisde that background process.
	//	So AsyncTask provides pbulishProgress(...) and onProgressUpdate(....).
	// Call publishProgress(..) from doInBackground(...) in the background thread.
	//	This will make onProgressUpdate*...) be called on the UI thread.
	//	Can do UI updates in onProgressUpdate*....), but control them from doInBackgroud(...) with publishProgress(...)
	
	// Third parameter in the list is the type of result produced by AsyncTask
	//	It sets the type of value returned by doInBackground(..), 
	//	as well as the type of onPostExecute(...)'s input parameter
	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
		
		// protected Void doInBackground(Void... params) {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			/* try {
				String result = new FlickrFetchr().getUrl("http://www.google.com");
				Log.i(TAG, "Fetched contents of URL: " + result);
			} catch (IOException ioe) {
				Log.e(TAG, "Failed to fetch URL: ", ioe);
			}
			*/
			//String query = "bird"; // Just for testing
			
			Activity activity = getActivity();
			if (activity == null)
				return new ArrayList<GalleryItem>();
			
			// Fetch search query from the default SharedPreferences.
			// Use appropriate method type to access data (getString(...), getInt(...), etc.)
			String query = PreferenceManager.getDefaultSharedPreferences(activity).getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			
			//^Preferences is the entire persistence for PhotoGallery. Much easier than serializing JSON.
			
			if (query != null) {
				return new FlickrFetchr().search(query);
			} else {
				return new FlickrFetchr().fetchItems();
			}
		}
		
		// onPostExecute accepts the list fetched inside doInBackground(...), puts it in mItems, and updates GridView's adapter
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems = items;
			setupAdapter();
		}
	}
	
	// GridView has no handy GridFragment class -- must build own adapter management code.
	// Use method setupAdapter() to do so.
	// setupAdapter looks at the current model state and configures the adapter appropriately on GridView.
	// Will want to call this in onCreateView(...) so that every time a new GridView is created on rotation, 
	//   it is reconfigured with an appropriate adapter.
	// Will also want to call it every time the set of model objects changes.
	void setupAdapter() {
		if (getActivity() == null || mGridView == null) return; // Check to make sure there is an activity before attaching adapter.
		
		if (mItems != null) {
			//mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, mItems));
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}

	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
			}
			
			ImageView imageView = (ImageView)convertView.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.brian_up_close);
			
			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.getUrl());
			
			return convertView;	
		}
	}
	
	// It is critical that HandlerThreads are explicitly destroyed, else they will never die. Like zombies. Or rocks.
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed");
	}
	
	// Clean out downloader when view is destroyed
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}
	
	@Override
	@TargetApi(11)
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			// Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView)searchItem.getActionView();
			
			// Get the data from out searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			searchView.setSearchableInfo(searchInfo);
		}
	}
	
	@Override
	@TargetApi(11)
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_search:
				getActivity().onSearchRequested();
				return true;
			case R.id.menu_item_clear:
				PreferenceManager.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
					.commit();
				updateItems();
				return true;
			case R.id.menu_item_toggle_polling:
				boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
				PollService.setServiceAlarm(getActivity(),  shouldStartAlarm);
				
				// Manually tell action bar to refresh its items/update itself
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					getActivity().invalidateOptionsMenu();
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		// Sufficient for pre-3.0 devices (for updating title)
		// Further implementation required for post-3.0. Need to manutally tell action bar to call 
		// onPrepareOptionsMenu(menu) and refresh its items by calling Acitivty.invalidateOPtionsMenu()
		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		} else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}
}
