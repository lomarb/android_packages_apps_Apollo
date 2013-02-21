package com.andrew.apollo.utils;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.widget.ImageView;

public class ImageUtils {

    private static ImageProvider imageProvider;

    public static final boolean DEBUG = false;

    public static void setArtistImage(ImageView imageView, String artist) {
        getImageProvider().setArtistImage(imageView, artist);
    }

    public static void setAlbumImage(ImageView imageView, String artist, String album) {
        getImageProvider().setAlbumImage(imageView, artist, album);
    }
    
    public static void deleteCache(Context context) throws IOException {
    	final File dir = context.getExternalFilesDir(null);
        final File[] files = dir.listFiles();
        for (final File file : files) {
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    private static ImageProvider getImageProvider() {
        if (imageProvider == null)
            imageProvider = new ImageProvider();
        return imageProvider;
    }
}
