package com.ocwvar.darkpurple.FragmentPages.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ocwvar.darkpurple.FragmentPages.work.PlaylistPageBackGround;
import com.ocwvar.darkpurple.R;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.FragmentPages
 * Data: 2016/8/14 18:33
 * Project: DarkPurple
 */
public class PlaylistPageFragment extends Fragment {

    private PlaylistPageBackGround workFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            return inflater.inflate(R.layout.fragment_playlist_page, container, false);
        } else {
            return null;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        workFragment = (PlaylistPageBackGround) getFragmentManager().findFragmentByTag(PlaylistPageBackGround.TAG);
        if (workFragment == null) {
            workFragment = new PlaylistPageBackGround();
            workFragment.setTargetFragment(this, 0);
            getFragmentManager().beginTransaction().add(workFragment, PlaylistPageBackGround.TAG).commit();
        } else {
            workFragment.setTargetFragment(this, 0);
        }

        workFragment.initData();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (workFragment != null) {
            workFragment.refreshData();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (workFragment != null) {
            workFragment.releaseData();
        }
    }

}
