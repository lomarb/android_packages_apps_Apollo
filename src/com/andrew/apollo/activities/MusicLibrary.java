/**
 * 
 */

package com.andrew.apollo.activities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Window;

import com.andrew.apollo.BottomActionBarControlsFragment;
import com.andrew.apollo.BottomActionBarFragment;
import com.andrew.apollo.IApolloService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.adapters.ScrollingTabsAdapter;
import com.andrew.apollo.grid.fragments.AlbumsFragment;
import com.andrew.apollo.grid.fragments.ArtistsFragment;
import com.andrew.apollo.list.fragments.GenresFragment;
import com.andrew.apollo.list.fragments.PlaylistsFragment;
import com.andrew.apollo.list.fragments.RecentlyAddedFragment;
import com.andrew.apollo.list.fragments.TracksFragment;
import com.andrew.apollo.service.ApolloService;
import com.andrew.apollo.service.ServiceToken;
import com.andrew.apollo.ui.widgets.ScrollableTabView;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.ImageCache;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.ThemeUtils;

import static com.andrew.apollo.Constants.MIME_TYPE;
import static com.andrew.apollo.Constants.PLAYLIST_RECENTLY_ADDED;
import static com.andrew.apollo.Constants.THEME_ITEM_BACKGROUND;
import static com.andrew.apollo.Constants.TABS_ENABLED;

/**
 * @author Andrew Neal
 * @Note This is the "holder" for all of the tabs
 */
public class MusicLibrary extends FragmentActivity implements ServiceConnection {

    private ServiceToken mToken;

    @Override
    protected void onCreate(Bundle icicle) {
        // Landscape mode on phone isn't ready
        if (!ApolloUtils.isTablet(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Scan for music
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Control Media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Layout
        setContentView(R.layout.library_browser);

        // Hide the ActionBar
        getActionBar().hide();

        // Update the BottomActionBar
        initBottomActionBar();
        super.onCreate(icicle);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    @Override
    protected void onStart() {

        // Important!
        initPager();
        
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);

        //TODO: clear image cache

        super.onStop();
    }

    /**
     * Initiate ViewPager and PagerAdapter
     */
    public void initPager() {
        // Initiate PagerAdapter
        PagerAdapter mPagerAdapter = new PagerAdapter(getSupportFragmentManager());

        Bundle bundle = new Bundle();
        bundle.putString(MIME_TYPE, Audio.Playlists.CONTENT_TYPE);
        bundle.putLong(BaseColumns._ID, PLAYLIST_RECENTLY_ADDED);
        
        //Get tab visibility preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> defaults = new HashSet<String>(Arrays.asList(
        		getResources().getStringArray(R.array.tab_titles)
        	));
        Set<String> tabs_set = sp.getStringSet(TABS_ENABLED,defaults);
        //if its empty fill reset it to full defaults
        	//stops app from crashing when no tabs are shown
        	//TODO:rewrite activity to not crash when no tabs are chosen to show
        if(tabs_set.size()==0)
        	tabs_set = defaults;
        
        //Only show tabs that were set in preferences
        // Recently added tracks
        if(tabs_set.contains("Recent"))
        	mPagerAdapter.addFragment(new RecentlyAddedFragment(bundle));
        // Artists
        if(tabs_set.contains("Artists"))
        	mPagerAdapter.addFragment(new ArtistsFragment());
        // Albums
        if(tabs_set.contains("Albums"))
        	mPagerAdapter.addFragment(new AlbumsFragment());
        // // Tracks
        if(tabs_set.contains("Songs"))
        	mPagerAdapter.addFragment(new TracksFragment());
        // // Playlists
        if(tabs_set.contains("Playlists"))
        	mPagerAdapter.addFragment(new PlaylistsFragment());
        // // Genres
        if(tabs_set.contains("Genres"))
        	mPagerAdapter.addFragment(new GenresFragment());

        // Initiate ViewPager
        ViewPager mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setPageMargin(getResources().getInteger(R.integer.viewpager_margin_width));
        mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(1);

        // Tabs
        initScrollableTabs(mViewPager);

        // Theme chooser
        ThemeUtils.initThemeChooser(this, mViewPager, "viewpager", THEME_ITEM_BACKGROUND);
        ThemeUtils.setMarginDrawable(this, mViewPager, "viewpager_margin");
    }

    /**
     * Initiate the tabs
     */
    public void initScrollableTabs(ViewPager mViewPager) {
        ScrollableTabView mScrollingTabs = (ScrollableTabView)findViewById(R.id.scrollingTabs);
        ScrollingTabsAdapter mScrollingTabsAdapter = new ScrollingTabsAdapter(this);
        mScrollingTabs.setAdapter(mScrollingTabsAdapter);
        mScrollingTabs.setViewPager(mViewPager);

        // Theme chooser
        ThemeUtils.initThemeChooser(this, mScrollingTabs, "scrollable_tab_background",
                THEME_ITEM_BACKGROUND);
    }

    /**
     * Initiate the BottomActionBar
     */
    private void initBottomActionBar() {
        PagerAdapter pagerAdatper = new PagerAdapter(getSupportFragmentManager());
        pagerAdatper.addFragment(new BottomActionBarFragment());
        pagerAdatper.addFragment(new BottomActionBarControlsFragment());
        ViewPager viewPager = (ViewPager)findViewById(R.id.bottomActionBarPager);
        viewPager.setAdapter(pagerAdatper);
    }
}
