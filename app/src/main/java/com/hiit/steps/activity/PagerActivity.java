package com.hiit.steps.activity;


import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.google.android.gms.maps.SupportMapFragment;
import com.hiit.steps.R;

public class PagerActivity extends FragmentActivity {

    ViewPager mPager;
    PagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pager);

        mAdapter = new PagerAdapter(this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        final ActionBar actionBar = getActionBar();
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // show the given tab
                mPager.setCurrentItem(tab.getPosition(), true);
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };

        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        for (int i = 0; i < mAdapter.getCount(); ++i) {
            actionBar.addTab(actionBar.newTab().setText(mAdapter.tabs[i].name).setTabListener(tabListener));
        }

    }

    private static class PagerAdapter extends FragmentPagerAdapter {

        private FragmentActivity activity;

        public static class Tab {
            public String name;
            public Class<? extends Fragment> fragment;
            public Tab(String name, Class<? extends Fragment> fragment) {
                this.name = name;
                this.fragment = fragment;
            }
            public static Tab get(String name, Class<? extends Fragment> fragment) {
                return new Tab(name, fragment);
            }
        }

        private Tab[] tabs = {
                Tab.get("Control", ControlFragment.class),
                Tab.get("Monitor", MonitorFragment.class),
                Tab.get("Map", SupportMapFragment.class)
        };

        public PagerAdapter(FragmentActivity activity) {
            super(activity.getSupportFragmentManager());
            this.activity = activity;
        }

        @Override
        public Fragment getItem(int i) {
            return Fragment.instantiate(activity, tabs[i].fragment.getName());
        }

        @Override
        public int getCount() {
            return tabs.length;
        }
    }

}
