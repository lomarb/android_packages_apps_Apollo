package com.andrew.apollo.tasks;

import static com.andrew.apollo.Constants.LASTFM_API_KEY;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.util.Log;

import com.andrew.apollo.lastfm.api.Album;
import com.andrew.apollo.lastfm.api.Artist;
import com.andrew.apollo.lastfm.api.Image;
import com.andrew.apollo.lastfm.api.ImageHolder;
import com.andrew.apollo.lastfm.api.ImageSize;
import com.andrew.apollo.lastfm.api.PaginatedResult;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.ImageUtils;
import static com.andrew.apollo.Constants.ALBUM_SPLITTER;
import static com.andrew.apollo.Constants.ALBUM_SUFFIX;
import static com.andrew.apollo.Constants.ARTIST_SUFFIX;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Set;

public class GetBitmapTask extends AsyncTask<String, Integer, Bitmap> {

    private static final String TAG = "GetBitmapTask";

    private static final String EXTENSION_JPG = ".jpg";
    private static final String EXTENSION_PNG = ".png";
    private static final String EXTENSION_GIF = ".gif";

    private static final String[] IMAGE_EXTENSIONS = new String[]{EXTENSION_JPG, EXTENSION_PNG, EXTENSION_GIF};

    private WeakReference<OnBitmapReadyListener> mListenerReference;

    private WeakReference<Context> mContextReference;

    private String mType;
    
    private String[] mTags;

    public GetBitmapTask(String type, String[] tags, OnBitmapReadyListener listener, Context context) {
        mListenerReference = new WeakReference<OnBitmapReadyListener>(listener);
        mContextReference = new WeakReference<Context>(context);
        mTags = tags;
        mType = type;
    }

    @Override
    protected Bitmap doInBackground(String... ignored) {
        Context context = mContextReference.get();
        if (context == null) {
            return null;
        }

        File file = findCachedFile(context);
        
        if(file == null && mType.equals("album"))
        	file = findMediaStoreFile(context);
        
        if (file == null && (mType.equals("album") || mType.equals("artist"))) {
            file = downloadImage(context);
        }

        if (file == null) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        if (bitmap == null) {
            return null;
        }

        return bitmap;
    }
    
    private File findMediaStoreFile(Context context){
    	String mAlbum = mTags[0];
    	String[] projection = {
                BaseColumns._ID, Audio.Albums._ID, Audio.Albums.ALBUM_ART, Audio.Albums.ALBUM
        };
        Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor cursor = null;
        try{
        	cursor = context.getContentResolver().query(uri, projection, Audio.Albums.ALBUM+ "=" + DatabaseUtils.sqlEscapeString(mAlbum), null, null);
        }
        catch(Exception e){
        	e.printStackTrace();        	
        }
        int column_index = cursor.getColumnIndex(Audio.Albums.ALBUM_ART);
        if(cursor.getCount()>0){
	    	cursor.moveToFirst();
	        String albumArt = cursor.getString(column_index);	  
	        if(albumArt != null){
		        File file = getFile(context, EXTENSION_PNG);
		        FileOutputStream out = null;
				try {
						Bitmap bmp = BitmapFactory.decodeFile(albumArt);
				        out = new FileOutputStream(file);
				        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
				        out.flush();
				        out.close();
				        cursor.close();
				        return file;
				} catch (Exception e) {
				        e.printStackTrace();
				}  
        	}
        }
        return null;
    }

    protected String getImageUrl() {
        try {
	        if(mType.equals("album")){    
	        	
	        	Album image = Album.getInfo(mTags[0], mTags[1], LASTFM_API_KEY);
	            if (image == null) {
	                return null;
	            }
	            Set<ImageSize> sizes = image.availableSizes();
		        return returnLargestImage(image, sizes);
	        }
	        else if (mType.equals("artist")) {
		        PaginatedResult<Image> images = Artist.getImages(mTags[0], 2, 1, LASTFM_API_KEY);
	            Iterator<Image> iterator = images.getPageResults().iterator();
	            if (!iterator.hasNext()) {
	                return null;
	            }
	            Image image = iterator.next();	
	            Set<ImageSize> sizes = image.availableSizes();
		        return returnLargestImage(image, sizes);
	        }
        } catch (Exception e) {
            if (ImageUtils.DEBUG) Log.w(TAG, "Error when retrieving image url", e);
            return null;
        }
        return null;
    }
    
    private String returnLargestImage(ImageHolder entry , Set<ImageSize> sizes){
        if(sizes.contains(ImageSize.MEGA))
        	return entry.getImageURL(ImageSize.MEGA);
        else if(sizes.contains(ImageSize.EXTRALARGE))
        	return entry.getImageURL(ImageSize.EXTRALARGE);
        else if(sizes.contains(ImageSize.LARGE))
        	return entry.getImageURL(ImageSize.LARGE);    	
        else if(sizes.contains(ImageSize.MEDIUM))
        	return entry.getImageURL(ImageSize.MEDIUM);  
        else
        	return entry.getImageURL(ImageSize.SMALL);
    }

    protected File getFile(Context context, String extension) {
    	String nTag = null;
        if(mType.equals("album")){  
        	nTag = ApolloUtils.escapeForFileSystem(mTags[0]+ALBUM_SPLITTER+mTags[1]+ALBUM_SUFFIX);
        }
        else if (mType.equals("artist")) {
        	nTag = ApolloUtils.escapeForFileSystem(mTags[0]+ARTIST_SUFFIX);
        }
        if (nTag == null) {
            return null;
        }
        return new File(context.getExternalFilesDir(null), nTag + extension);
    }

    protected File findCachedFile(Context context) {
        for (String extension : IMAGE_EXTENSIONS) {
            File file = getFile(context, extension);
            if (file == null) {
                return null;
            }
            if (file.exists()) {
                if (ImageUtils.DEBUG) Log.d(TAG, "Cached file found: " + file.getAbsolutePath());
                return file;
            }
        }        
        return null;
    }

    private File downloadImage(Context context) {
        String url = getImageUrl();
        if (url == null || url.isEmpty()) {
            return null;
        }
        File file = getFile(context, getExtension(url));
        if (ImageUtils.DEBUG) Log.v(TAG, "Downloading " + url + " to " + file.getAbsolutePath());
        ApolloUtils.downloadFile(url, file);
        if (file.exists()) {
            return file;
        }
        if (ImageUtils.DEBUG) Log.w(TAG, "Error downloading a " + url + " to " + file.getAbsolutePath());
        return null;
    }

    protected String getExtension(String url) {
        for (String extension : IMAGE_EXTENSIONS) {
            if (url.endsWith(extension))
                return extension;
        }
        return EXTENSION_JPG;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        OnBitmapReadyListener listener = mListenerReference.get();
        if (bitmap != null) {
            if (listener != null) {
            	if(mType.equals("album")){
                    listener.bitmapReady(bitmap, mTags[0]+ALBUM_SPLITTER+mTags[1]+ALBUM_SUFFIX);  
                }
                else if (mType.equals("artist")) {
                	listener.bitmapReady(bitmap, mTags[0]+ARTIST_SUFFIX);  
                }
            }
        }
    }

    public static interface OnBitmapReadyListener {
        public void bitmapReady(Bitmap bitmap, String tag);
    }
}
