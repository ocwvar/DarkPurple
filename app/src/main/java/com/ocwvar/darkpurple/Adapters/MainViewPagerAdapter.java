package com.ocwvar.darkpurple.Adapters;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.ocwvar.darkpurple.FragmentPages.AllMusicFragment;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/7/5 16:04
 * Project: DarkPurple
 * 主页ViewPager适配器
 */
public class MainViewPagerAdapter extends FragmentPagerAdapter {

    private String[] titles;
    private ArrayList<Fragment> fragmentPages;

    public MainViewPagerAdapter(@NonNull FragmentManager fm ,@NonNull String[] titles) {
        super(fm);
        this.titles = titles;
        fragmentPages = new ArrayList<>();
    }

    public void addFragmentPageToStart(Fragment fragment){
        addFragmentPage(fragment,0);
    }

    public void addFragmentPageToEnd(Fragment fragment){
        fragmentPages.add(fragment);
        notifyDataSetChanged();
    }

    public void addFragmentPage(Fragment fragment , int index){
        if (fragment != null && (fragmentPages.size() == 0 && index >= 0) || (fragmentPages.size() > 0 && index >=0 && index < fragmentPages.size())){
            fragmentPages.add(index,fragment);
            notifyDataSetChanged();
        }
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentPages.get(position);
    }

    @Override
    public int getCount() {
        return fragmentPages.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position < titles.length && position >= 0){
            return titles[position];
        }else {
            return null;
        }
    }

}
