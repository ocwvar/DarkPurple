/*
	BASS EQ effects test
	Copyright (c) 2001-2012 Un4seen Developments Ltd.
*/

package com.un4seen.bass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import java.io.File;
import java.lang.Math;

import com.ocwvar.darkpurple.R;
import com.un4seen.bass.BASS;

public class FXTest extends Activity {
	int chan;				// channel handle
	int[] fx=new int[4];	// 3 eq bands + reverb

	File filepath;
	String[] filelist;

	class RunnableParam implements Runnable {
		Object param;
		RunnableParam(Object p) { param=p; }
		public void run() {}
	}
	
	// display error messages
	void Error(String es) {
		// get error code in current thread for display in UI thread
		String s=String.format("%s\n(error code: %d)", es, BASS.BASS_ErrorGetCode());
		runOnUiThread(new RunnableParam(s) {
            public void run() {
        		new AlertDialog.Builder(FXTest.this)
    				.setMessage((String)param)
    				.setPositiveButton("OK", null)
    				.show();
            }
		});
	}

	public void UpdateFX(SeekBar sb) {
		int v=sb.getProgress();
		int n=Integer.parseInt((String)sb.getTag());
		if (n<3) {
			BASS.BASS_DX8_PARAMEQ p=new BASS.BASS_DX8_PARAMEQ();
			BASS.BASS_FXGetParameters(fx[n], p);
			p.fGain=v-10;
			BASS.BASS_FXSetParameters(fx[n], p);
		} else {
			BASS.BASS_DX8_REVERB p=new BASS.BASS_DX8_REVERB();
			BASS.BASS_FXGetParameters(fx[n], p);
			p.fReverbMix=(float)(v!=0?Math.log(v/20.0)*20:-96);
			BASS.BASS_FXSetParameters(fx[n], p);
		}
	}

	public void OpenClicked(View v) {
		String[] list=filepath.list();
		if (list==null) list=new String[0];
		if (!filepath.getPath().equals("/")) {
			filelist=new String[list.length+1];
			filelist[0]="..";
			System.arraycopy(list, 0, filelist, 1, list.length);
		} else
			filelist=list;
        new AlertDialog.Builder(this)
			.setTitle("Choose a file to play")
			.setItems(filelist, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					File sel;
					if (filelist[which].equals("..")) sel=filepath.getParentFile();
					else sel=new File(filepath, filelist[which]);
					if (sel.isDirectory()) {
						filepath=sel;
						OpenClicked(null);
					} else {
						String file=sel.getPath();
						// first free the current one (try both MOD and stream - it must be one of them)
						BASS.BASS_MusicFree(chan);
						BASS.BASS_StreamFree(chan);
						if ((chan=BASS.BASS_StreamCreateFile(file, 0, 0, BASS.BASS_SAMPLE_LOOP))==0
							&& (chan=BASS.BASS_MusicLoad(file, 0, 0, BASS.BASS_SAMPLE_LOOP|BASS.BASS_MUSIC_RAMP, 1))==0) {
							// whatever it is, it ain't playable
							((Button)findViewById(R.id.open)).setText("press here to open a file");
							Error("Can't play the file");
							return;
						}
						((Button)findViewById(R.id.open)).setText(file);
						// setup the effects and start playing
						fx[0]=BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_PARAMEQ, 0);
						fx[1]=BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_PARAMEQ, 0);
						fx[2]=BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_PARAMEQ, 0);
						fx[3]=BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_REVERB, 0);
						BASS.BASS_DX8_PARAMEQ p=new BASS.BASS_DX8_PARAMEQ();
						p.fGain=0;
						p.fBandwidth=18;
						p.fCenter=125;
						BASS.BASS_FXSetParameters(fx[0], p);
						p.fCenter=1000;
						BASS.BASS_FXSetParameters(fx[1], p);
						p.fCenter=8000;
						BASS.BASS_FXSetParameters(fx[2], p);
						UpdateFX((SeekBar)findViewById(R.id.eq1));
						UpdateFX((SeekBar)findViewById(R.id.eq2));
						UpdateFX((SeekBar)findViewById(R.id.eq3));
						UpdateFX((SeekBar)findViewById(R.id.reverb));
						BASS.BASS_ChannelPlay(chan, false);
					}
				}
			})
	   		.show();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		filepath=Environment.getExternalStorageDirectory();

		// initialize default output device
		if (!BASS.BASS_Init(-1, 44100, 0)) {
			Error("Can't initialize device");
			return;
		}

		// enable floating-point (actually 8.24 fixed-point) DSP/FX
		BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1);

        SeekBar.OnSeekBarChangeListener osbcl=new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				UpdateFX(seekBar);
			}
		};
		((SeekBar)findViewById(R.id.eq1)).setOnSeekBarChangeListener(osbcl);
		((SeekBar)findViewById(R.id.eq2)).setOnSeekBarChangeListener(osbcl);
		((SeekBar)findViewById(R.id.eq3)).setOnSeekBarChangeListener(osbcl);
		((SeekBar)findViewById(R.id.reverb)).setOnSeekBarChangeListener(osbcl);
    }
    
    @Override
    public void onDestroy() {
    	BASS.BASS_Free();

    	super.onDestroy();
    }
}