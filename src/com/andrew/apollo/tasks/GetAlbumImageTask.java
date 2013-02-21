package com.andrew.apollo.tasks;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.util.Log;
import com.andrew.apollo.lastfm.api.Album;
import com.andrew.apollo.lastfm.api.ImageSize;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;

import static com.andrew.apollo.Constants.LASTFM_API_KEY;

public class GetAlbumImageTask extends GetBitmapTask {

    private final String TAG = "GetArtistImageTask";

    private static final String EXTENSION_JPG = ".jpg";
    private static final String EXTENSION_PNG = ".png";
    private static final String EXTENSION_GIF = ".gif";

    private static final String[] IMAGE_EXTENSIONS = new String[]{EXTENSION_JPG, EXTENSION_PNG, EXTENSION_GIF};

    private String mArtist;

    private String mAlbum;

    public GetAlbumImageTask(String artist, String album, OnBitmapReadyListener listener, String tag, Context context) {
        super(listener, tag, context);
        mArtist = artist;
        mAlbum = album;
    }

    @Override
    protected File getFile(Context context, String extension) {
        String albumPart = ApolloUtils.escapeForFileSystem(mAlbum);
        String artistPart = ApolloUtils.escapeForFileSystem(mArtist);

        if (albumPart == null || artistPart == null) {
            Log.e(TAG, "Can't create file name for: " + mAlbum + " " + mArtist);
            return null;
        }

        return new File(context.getExternalFilesDir(null), artistPart + " - " + albumPart + extension);
    }
    
    @Override
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
        
        //Should only reach this point the first time it is being called for a certain album
        	//subsequent calls should retrieve cached image before we get here
        //Since its the first run, pull the album image from androids mediastore if it exists
        	//if it fails then it will go through apollos normal mode of operation and grab
        	//album art from Last.fm
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

    @Override
    protected String getImageUrl() {
        try {
            Album album = Album.getInfo(mArtist, this.mAlbum, LASTFM_API_KEY);
            if (album == null) {
                if (ImageUtils.DEBUG) Log.w(TAG, "Album not found: " + mArtist + " - " + this.mAlbum);
                return null;
            }
            Set<ImageSize> sizes = album.availableSizes();
            if(sizes.contains(ImageSize.MEGA))
            	return album.getImageURL(ImageSize.MEGA);
            else if(sizes.contains(ImageSize.EXTRALARGE))
            	return album.getImageURL(ImageSize.EXTRALARGE);
            else 
            	return album.getImageURL(ImageSize.LARGE);
        } catch (Exception e) {
            if (ImageUtils.DEBUG) Log.w(TAG, "Error when retrieving album image url", e);
            return null;
        }
    }
}
