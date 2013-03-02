/**
 * 
 */

package com.andrew.apolloMod.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.andrew.apolloMod.IApolloService;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.helpers.utils.ThemeUtils;
import com.andrew.apolloMod.ui.adapters.PagerAdapter;
import com.andrew.apolloMod.ui.fragments.list.ArtistAlbumsFragment;
import com.andrew.apolloMod.ui.fragments.list.TracksFragment;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.service.ServiceToken;

import static com.andrew.apolloMod.Constants.*;

/**
 * @author Andrew Neal
 * @Note This displays specific track or album listings
 */
public class TracksBrowser extends Activity implements ServiceConnection {

    // Bundle
    private Bundle bundle;

    private Intent intent;

    private String mimeType;

    private ServiceToken mToken;
    
    private int RESULT_LOAD_IMAGE = 1;
    
    private ImageProvider mImageProvider;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Landscape mode on phone isn't ready
        if (!ApolloUtils.isTablet(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Control Media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Layout
        setContentView(R.layout.track_browser);
        registerForContextMenu(findViewById(R.id.half_artist_image));

    	mImageProvider = ImageProvider.getInstance( this );

        // Important!
        whatBundle(icicle);

        // Update the colorstrip color
        initColorstrip();

        // Update the ActionBar
        initActionBar();

        // Update the half_and_half layout
        initUpperHalf();

        // Important!
        initPager();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    	if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
    		
        	menu.setHeaderTitle(R.string.image_edit_artists);
        	getMenuInflater().inflate(R.menu.context_artistimage, menu); 
        	
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
        	
        	menu.setHeaderTitle(R.string.image_edit_albums);
        	getMenuInflater().inflate(R.menu.context_albumimage, menu); 
        	
        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
        	
        	menu.setHeaderTitle(R.string.image_edit_playlist);
        	getMenuInflater().inflate(R.menu.context_playlist_genreimage, menu); 
        	
        }
        else{
        	
        	menu.setHeaderTitle(R.string.image_edit_genre);
        	getMenuInflater().inflate(R.menu.context_playlist_genreimage, menu); 
        	
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	ImageInfo mInfo = null;      
        switch (item.getItemId()) {
            case R.id.image_edit_gallery:
            	Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            	startActivityForResult(i, RESULT_LOAD_IMAGE);
            	return true;
            case R.id.image_edit_file:            	
                mInfo = new ImageInfo();
                mInfo.type = TYPE_ALBUM;
                mInfo.size = SIZE_NORMAL;
                mInfo.source = SRC_FILE;
                mInfo.data = new String[]{ getAlbumId(), getArtist(), getAlbum() };                
                mImageProvider.loadImage((ImageView)findViewById(R.id.half_artist_image), mInfo );
                return true;
            case R.id.image_edit_lastfm:           	
                mInfo = new ImageInfo();
                mInfo.size = SIZE_NORMAL;
                mInfo.source = SRC_LASTFM;                
    	        if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) { 
                    mInfo.type = TYPE_ARTIST;
                    mInfo.data = new String[]{ getArtist() };
    	        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
                    mInfo.type = TYPE_ALBUM;
                    mInfo.data = new String[]{ getAlbumId(), getArtist(), getAlbum() };
    	        } 
                mImageProvider.loadImage((ImageView)findViewById(R.id.half_artist_image), mInfo );
                return true;
            case R.id.image_edit_web:
            	onSearchWeb();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    public void onSearchWeb(){
    	String query = "";
    	if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
    		query = getArtist();
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
        	query = getAlbum() + " " + getArtist();
        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
        	query = bundle.getString(PLAYLIST_NAME);
        }
        else{
            Long id = bundle.getLong(BaseColumns._ID);
            query = MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true));
        }
        final Intent googleSearch = new Intent(Intent.ACTION_WEB_SEARCH);
        googleSearch.putExtra(SearchManager.QUERY, query);
        startActivity(googleSearch);	
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK && requestCode == RESULT_LOAD_IMAGE  && data != null)
	    {
        	Uri selectedImage = data.getData();
	        String[] filePathColumn = { MediaStore.Images.Media.DATA };
	        Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
	        cursor.moveToFirst();
	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        String picturePath = cursor.getString(columnIndex);
	        cursor.close();
        	
            ImageInfo mInfo = new ImageInfo();
	        if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) { 
	            mInfo.type = TYPE_ARTIST;
	            mInfo.data = new String[]{ getArtist(), picturePath };    
	        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
	            mInfo.type = TYPE_ALBUM;
	            mInfo.data = new String[]{ getAlbumId(), getAlbum(), getArtist(), picturePath };
	        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
	            mInfo.type = TYPE_PLAYLIST;
	            mInfo.data = new String[]{ bundle.getString(PLAYLIST_NAME), picturePath };
	        }
	        else{ 
	        	Long id = bundle.getLong(BaseColumns._ID);
	            mInfo.type = TYPE_GENRE;
	            mInfo.data = new String[]{  MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true)), picturePath };
	        }
	        
            mInfo.size = SIZE_NORMAL;
            mInfo.source = SRC_GALLERY;          
            mImageProvider.loadImage((ImageView)findViewById(R.id.half_artist_image), mInfo );
	        
	    }
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putAll(bundle);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    /**
     * Update next BottomActionBar as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	
        }

    };

    @Override
    protected void onStart() {
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        registerReceiver(mMediaStatusReceiver, filter);

        setTitle();
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);

        unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                if(bundle.getBoolean(UP_STARTS_ALBUM_ACTIVITY))
                {
                    // Artist ID
                    long artistID = ApolloUtils.getArtistId(getArtist(), ARTIST_ID, this);
                    if (ApolloUtils.isAlbum(mimeType) && artistID != 0)
                        tracksBrowser(artistID);
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @param icicle
     * @return what Bundle we're dealing with
     */
    public void whatBundle(Bundle icicle) {
        intent = getIntent();
        bundle = icicle != null ? icicle : intent.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        if (bundle.getString(INTENT_ACTION) == null) {
            bundle.putString(INTENT_ACTION, intent.getAction());
        }
        if (bundle.getString(MIME_TYPE) == null) {
            bundle.putString(MIME_TYPE, intent.getType());
        }
        mimeType = bundle.getString(MIME_TYPE);
    }

    /**
     * For the theme chooser
     */
    private void initColorstrip() {
        FrameLayout mColorstrip = (FrameLayout)findViewById(R.id.colorstrip);
        mColorstrip.setBackgroundColor(getResources().getColor(R.color.holo_blue_dark));
        ThemeUtils.setBackgroundColor(this, mColorstrip, "colorstrip");

        RelativeLayout mColorstrip2 = (RelativeLayout)findViewById(R.id.bottom_colorstrip);
        mColorstrip2.setBackgroundColor(getResources().getColor(R.color.holo_blue_dark));
        ThemeUtils.setBackgroundColor(this, mColorstrip2, "colorstrip");
    }

    /**
     * Set the ActionBar title
     */
    private void initActionBar() {
        ApolloUtils.showUpTitleOnly(getActionBar());

        // The ActionBar Title and UP ids are hidden.
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        int upId = Resources.getSystem().getIdentifier("up", "id", "android");

        TextView actionBarTitle = (TextView)findViewById(titleId);
        ImageView actionBarUp = (ImageView)findViewById(upId);

        // Theme chooser
        ThemeUtils.setActionBarBackground(this, getActionBar(), "action_bar_background");
        ThemeUtils.setTextColor(this, actionBarTitle, "action_bar_title_color");
        ThemeUtils.initThemeChooser(this, actionBarUp, "action_bar_up", THEME_ITEM_BACKGROUND);

    }

    /**
     * Sets up the @half_and_half.xml layout
     */
    private void initUpperHalf() {

        if (ApolloUtils.isArtist(mimeType)) {
            // Get next artist image
            final ImageView mFirstHalfImage = (ImageView)findViewById(R.id.half_artist_image);
          	
            ImageInfo mInfo = new ImageInfo();
            mInfo.type = TYPE_ARTIST;
            mInfo.size = SIZE_NORMAL;
            mInfo.source = SRC_FIRST_AVAILABLE;
            mInfo.data = new String[]{ getArtist() };                
            mImageProvider.loadImage( mFirstHalfImage, mInfo );
            
            findViewById(R.id.half_artist_info_holder).setVisibility(View.VISIBLE);

            // Album name
            TextView mArtistName = (TextView)findViewById(R.id.half_artist_image_text);
            mArtistName.setText(getArtist());
            String numAlbums = MusicUtils.makeAlbumsLabel(this, Integer.parseInt(getNumAlbums()), 0, false);
            mArtistName = (TextView)findViewById(R.id.half_artist_image_text_line_two);
            mArtistName.setText(numAlbums);
        } else if (ApolloUtils.isAlbum(mimeType)) {
            // Album image
            final ImageView mSecondHalfImage = (ImageView)findViewById(R.id.half_artist_image);

            ImageInfo mInfo = new ImageInfo();
            mInfo.type = TYPE_ALBUM;
            mInfo.size = SIZE_NORMAL;
            mInfo.source = SRC_FIRST_AVAILABLE;
            mInfo.data = new String[]{ getAlbumId(), getAlbum(), getArtist() };                
            mImageProvider.loadImage( mSecondHalfImage, mInfo );

            findViewById(R.id.half_artist_info_holder).setVisibility(View.VISIBLE);
            
            // Album name
            TextView mAlbumName = (TextView)findViewById(R.id.half_artist_image_text);
            mAlbumName.setText(getAlbum());

            mAlbumName = (TextView)findViewById(R.id.half_artist_image_text_line_two);
            mAlbumName.setText(getArtist());
           
        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
        	// Playlist image
            final ImageView mSecondHalfImage = (ImageView)findViewById(R.id.half_artist_image);
            String plyName = bundle.getString(PLAYLIST_NAME);
            
            ImageInfo mInfo = new ImageInfo();
            mInfo.type = TYPE_PLAYLIST;
            mInfo.size = SIZE_NORMAL;
            mInfo.source = SRC_FIRST_AVAILABLE;
            mInfo.data = new String[]{ plyName };               
            mImageProvider.loadImage( mSecondHalfImage, mInfo );

            findViewById(R.id.half_artist_info_holder).setVisibility(View.VISIBLE);

            TextView playlistName = (TextView)findViewById(R.id.half_artist_image_text);
            playlistName.setText( plyName );
        	
        }
        else{ 
        	

            final ImageView mSecondHalfImage = (ImageView)findViewById(R.id.half_artist_image);

        	String genName = MusicUtils.parseGenreName(this,
        			MusicUtils.getGenreName(this, bundle.getLong(BaseColumns._ID), true));
        	
            ImageInfo mInfo = new ImageInfo();
            mInfo.type = TYPE_GENRE;
            mInfo.size = SIZE_NORMAL;
            mInfo.source = SRC_FIRST_AVAILABLE;
            mInfo.data = new String[]{ genName };             
            mImageProvider.loadImage( mSecondHalfImage, mInfo );

            findViewById(R.id.half_artist_info_holder).setVisibility(View.VISIBLE);

            TextView genreName = (TextView)findViewById(R.id.half_artist_image_text);
            genreName.setText( genName );
        	
        	
        
        }
    }

    /**
     * Initiate ViewPager and PagerAdapter
     */
    private void initPager() {
        // Initiate PagerAdapter
        PagerAdapter mPagerAdapter = new PagerAdapter(getFragmentManager());
        if (ApolloUtils.isArtist(mimeType))
            // Show all albums for an artist
            mPagerAdapter.addFragment(new ArtistAlbumsFragment(bundle));
        // Show the tracks for an artist or album
        mPagerAdapter.addFragment(new TracksFragment(bundle));

        // Set up ViewPager
        ViewPager mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setPageMargin(getResources().getInteger(R.integer.viewpager_margin_width));
        mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        mViewPager.setAdapter(mPagerAdapter);

        // Theme chooser
        ThemeUtils.initThemeChooser(this, mViewPager, "viewpager", THEME_ITEM_BACKGROUND);
        ThemeUtils.setMarginDrawable(this, mViewPager, "viewpager_margin");
    }

    /**
     * @return artist name from Bundle
     */
    public String getArtist() {
        if (bundle.getString(ARTIST_KEY) != null)
            return bundle.getString(ARTIST_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return album name from Bundle
     */
    public String getAlbum() {
        if (bundle.getString(ALBUM_KEY) != null)
            return bundle.getString(ALBUM_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return album name from Bundle
     */
    public String getAlbumId() {
        if (bundle.getString(ALBUM_ID_KEY) != null)
            return bundle.getString(ALBUM_ID_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return number of albums from Bundle
     */
    public String getNumAlbums() {
        if (bundle.getString(NUMALBUMS) != null)
            return bundle.getString(NUMALBUMS);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return genre name from Bundle
     */
    public String getGenre() {
        if (bundle.getString(GENRE_KEY) != null)
            return bundle.getString(GENRE_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return playlist name from Bundle
     */
    public String getPlaylist() {
        if (bundle.getString(PLAYLIST_NAME) != null)
            return bundle.getString(PLAYLIST_NAME);
        return getResources().getString(R.string.app_name);
    }

    /**
     * Set the header when viewing a genre
     */
    public void setPromoImage() {

        // Artist image & Genre image
        ImageView mFirstHalfImage = (ImageView)findViewById(R.id.half_artist_image);

        Bitmap header = BitmapFactory.decodeResource(getResources(), R.drawable.promo);
        ApolloUtils.runnableBackground(mFirstHalfImage, header);
    }

    /**
     * Return here from viewing the tracks for an album and view all albums and
     * tracks for the same artist
     */
    private void tracksBrowser(long id) {
        bundle.putString(MIME_TYPE, Audio.Artists.CONTENT_TYPE);
        bundle.putString(ARTIST_KEY, getArtist());
        bundle.putLong(BaseColumns._ID, id);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, TracksBrowser.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Set the correct title
     */
    private void setTitle() {
        String name;
        long id;
        if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            switch ((int)id) {
                case (int)PLAYLIST_QUEUE:
                    setTitle(R.string.nowplaying);
                    return;
                case (int)PLAYLIST_FAVORITES:
                    setTitle(R.string.favorite);
                    return;
                default:
                    if (id < 0) {
                        setTitle(R.string.app_name);
                        return;
                    }
            }
            name = MusicUtils.getPlaylistName(this, id);
        } else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name =  getString (R.string.artist_page_title)+MusicUtils.getArtistName(this, id, true);
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name =  getString (R.string.album_page_title)+MusicUtils.getAlbumName(this, id, true);
        } else if (Audio.Genres.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name = MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true));
        } else {
            setTitle(R.string.app_name);
            return;
        }
        setTitle(name);
    }
}