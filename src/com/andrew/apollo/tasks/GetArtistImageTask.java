package com.andrew.apollo.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.andrew.apollo.lastfm.api.Artist;
import com.andrew.apollo.lastfm.api.Image;
import com.andrew.apollo.lastfm.api.ImageSize;
import com.andrew.apollo.lastfm.api.PaginatedResult;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.ImageUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import static com.andrew.apollo.Constants.LASTFM_API_KEY;

public class GetArtistImageTask extends GetBitmapTask {

    private final String TAG = "GetArtistImageTask";

    private static final String EXTENSION_JPG = ".jpg";
    private static final String EXTENSION_PNG = ".png";
    private static final String EXTENSION_GIF = ".gif";

    private static final String[] IMAGE_EXTENSIONS = new String[]{EXTENSION_JPG, EXTENSION_PNG, EXTENSION_GIF};

    private String mArtist;

    public GetArtistImageTask(String artist, OnBitmapReadyListener listener, String tag, Context context) {
        super(listener, tag, context);
        mArtist = artist;
    }

    @Override
    protected File getFile(Context context, String extension) {
        String fileName = ApolloUtils.escapeForFileSystem(mArtist);
        if (fileName == null) {
            Log.e(TAG, "Can't create file name for: " + mArtist);
            return null;
        }
        return new File(context.getExternalFilesDir(null), fileName + extension);
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
        return null;
    }
    
    @Override
    protected String getImageUrl() {
        try {
            PaginatedResult<Image> images = Artist.getImages(this.mArtist, 2, 1, LASTFM_API_KEY);
            Iterator<Image> iterator = images.getPageResults().iterator();
            if (!iterator.hasNext()) {
                if (ImageUtils.DEBUG) Log.w(TAG, "Error when retrieving artist image url for \"" + mArtist + "\" - empty result");
                return null;
            }
            Image image = iterator.next();

            Set<ImageSize> sizes = image.availableSizes();
            if(sizes.contains(ImageSize.MEGA))
            	return image.getImageURL(ImageSize.MEGA);
            else if(sizes.contains(ImageSize.EXTRALARGE))
            	return image.getImageURL(ImageSize.EXTRALARGE);
            else 
            	return image.getImageURL(ImageSize.LARGE);
        } catch (Exception e) {
            if (ImageUtils.DEBUG) Log.w(TAG, "Error when retrieving artist image url for \"" + mArtist + "\"", e);
            return null;
        }
    }
}
