package com.ocwvar.darkpurple.FragmentPages;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.Activities.PlayingActivity;
import com.ocwvar.darkpurple.R;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.FragmentPages
 * Data: 2016/7/5 16:03
 * Project: DarkPurple
 * 所有歌曲页面
 */
public class AllMusicFragment extends Fragment {

    private AllMusicBackGround workFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_all_music,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_allmusic_refresh:
                if (workFragment != null){
                    workFragment.refreshData();
                }
                break;
        }
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null && inflater != null){
            return inflater.inflate(R.layout.fragment_allmusic,container,false);
        }else {
            return null;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        workFragment = (AllMusicBackGround) getFragmentManager().findFragmentByTag(AllMusicBackGround.TAG);
        if (workFragment == null){
            workFragment = new AllMusicBackGround();
            workFragment.setTargetFragment(this,0);
            getFragmentManager().beginTransaction().add(workFragment,AllMusicBackGround.TAG).commit();
        }else{
            workFragment.setTargetFragment(this,0);
        }

        workFragment.initData();
        workFragment.refreshData();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        workFragment.releaseData();
    }

}
