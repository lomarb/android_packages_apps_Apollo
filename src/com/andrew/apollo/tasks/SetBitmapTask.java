package com.andrew.apollo.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.andrew.apollo.utils.ApolloUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

public class SetBitmapTask extends AsyncTask<String, Integer, Bitmap> {

    private WeakReference<OnBitmapReadyListener> mListenerReference;

    private WeakReference<Context> mContextReference;

    private String mPath, mTag;

    public SetBitmapTask(String path, String tag, OnBitmapReadyListener listener, Context context) {
        mListenerReference = new WeakReference<OnBitmapReadyListener>(listener);
        mContextReference = new WeakReference<Context>(context);
        mTag = tag;
        mPath = path;
    }

    @Override
    protected Bitmap doInBackground(String... ignored) {
        Context context = mContextReference.get();
        if (context == null) {
            return null;
        }
        deleteFromCache(context);

        File newFile = new File(context.getExternalFilesDir(null), ApolloUtils.escapeForFileSystem(mTag) + ".png");
    	FileOutputStream out = null;
		try {
				Bitmap bmp = BitmapFactory.decodeFile(mPath);
		        out = new FileOutputStream(newFile);
		        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
		        out.flush();
		        out.close();
		        return bmp;
		} catch (Exception e) {
		        e.printStackTrace();
		}
        return null;
    }
    
    protected void deleteFromCache(Context context){
    	String[] IMAGE_EXTENSIONS = new String[]{".jpg", ".png", ".gif"};
        for (String extension : IMAGE_EXTENSIONS) {
        	File oldFile = new File(context.getExternalFilesDir(null), ApolloUtils.escapeForFileSystem(mTag) + extension);
        	if(oldFile.exists())
        		oldFile.delete();
        }    	
    }
    
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        OnBitmapReadyListener listener = mListenerReference.get();
        if (bitmap != null) {
            if (listener != null) {
                    listener.bitmapReady(bitmap, mTag);
            }
        }
    }

    public static interface OnBitmapReadyListener {
        public void bitmapReady(Bitmap bitmap, String tag);
    }
}
