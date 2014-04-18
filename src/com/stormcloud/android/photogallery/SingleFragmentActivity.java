package com.stormcloud.android.photogallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public abstract class SingleFragmentActivity extends FragmentActivity {
	protected abstract Fragment createFragment();
	
	protected int getLayoutResId() {
		return R.layout.activity_fragment;
	}
	
	// Called when the activity is first created.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_fragment); //Currently hardcoded to inflate activity_fragment.xml
		setContentView(getLayoutResId()); //Enables a SingleFragmentActivity subclass to provide its own resource ID for layout instead
		
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
		
		if (fragment == null) {
			fragment = createFragment();
			fm.beginTransaction()
				.add(R.id.fragmentContainer, fragment)
				.commit();
		}
	}
}
