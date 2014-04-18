package com.stormcloud.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {
	public static final String TAG = "FlickrFetchr";
	
	// Prefix PREF stands for Preferences of SharedPreferences, which acts like a key-value. PREF_ is the key for some value
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";
	
	private static final String ENDPOINT = "http://api.flickr.com/services/rest/";
	private static final String API_KEY = "1156c9f2adef8a2cf88010a643599544";
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	private static final String METHOD_SEARCH = "flickr.photos.search";
	private static final String PARAM_EXTRAS = "extras";
	private static final String PARAM_TEXT = "text";
	
	private static final String XML_PHOTO = "photo";
	
	private static final String EXTRA_SMALL_URL = "url_s"; //url_s extra tells Flickr to include the URL for the small version of the picture if it is available
	
	byte[] getUrlBytes(String urlSpec) throws IOException {
		
		// Create a URL object from a string - e.g. http://www.google.com
		// Then calls openConnection() to create a connection object pointed at the URL
		// URL.openConnection() returns a URLConnection, but since you are connecting to an http URL,
		// you can cast it to HttpURLConnection.
		//   This gives you HTTP-specific interfaces for working with request methods, response codes, streaming methods, and more.
		
		// HttpURLConnection represents a connection, but it will not actually connect to your endpoint
		// until you call getInputStream() (or getOutputStream() for POST calls).
		// Only then can you get a valid response code.
		
		// Once the URL is created and connection opened, read() will be called repeatedly until the connection
		// runs out of data.
		
		// The inputStream will yield bytes as they are available.
		// When done, close the stream and spit out the ByteArrayOutputStream's byte array.
		
		
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();
			
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}
	
	// Converts bytes fetched by getUrlBytes(String) into a String.
	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	
	// Use Uri.Builder to build the complete URL for the Flickr API request.
	// Uri.Builder is a convenience class for creating properly escaped parameterized URLs.
	// Uri.Builder.appendQueryParameter(String,String) will automatically escape query strings for you.
	// Now, modift AsyncTask in PhotoGalleryFragment to all the new fetchItems() method.
	
	// Old header
	// public void fetchItems() {
	
	public ArrayList<GalleryItem> downloadGalleryItems(String url) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
				
		try {
			
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			parseItems(items, parser);
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fet items", ioe);
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to parse items", xppe);
		}
		return items;
	}
	
	public ArrayList<GalleryItem> fetchItems() {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS,  EXTRA_SMALL_URL)
				.build().toString();
		return downloadGalleryItems(url);
	}
	
	public ArrayList<GalleryItem> search (String query) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		return downloadGalleryItems(url);
	}
	
	// Imagine that XmlPullParser as having its finger on the XML document, walking step by step through
	// different events like START_TAG, END_TAG, and END_DOCUMENT.
	// At each step, methods can be called to answer any question you have about the event XmlPullParser currently has it's finger on
	//   e.g. getText(), getName(), or getAttributeValue(...).
	
	// To move the finger to the next interesting event in the XML, call next()
	
	void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException {
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				String owner = parser.getAttributeValue(null, "owner");
				
				GalleryItem item = new GalleryItem();
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				item.setOwner(owner);
				items.add(item);
			}
			
			eventType = parser.next();
		}
	}

}
