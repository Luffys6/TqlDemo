package com.sonix.oidbluetooth;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

/**
 * 根据笔返回的点读码处理数据
 */
public class ElementActivity extends Activity {
    private final static String TAG = "ElementActivity";

    private int elementCode = -1;
    private ImageView imageView;
    private MediaPlayer mediaPlayer;
    private int count = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_element);

        Log.i(TAG, "onCreate: ");
        imageView = findViewById(R.id.iv_element);

        Intent intent = this.getIntent();
        if (intent != null) {
            elementCode = intent.getIntExtra("value", -1);
        }

        if (elementCode != -1) {
            switch (elementCode) {
                case 1:
                    imageView.setImageResource(R.drawable.egg);
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.rice);
                    break;
                case 3:
                    imageView.setImageResource(R.drawable.bread);
                    break;
                default:
                    break;
            }
        }

        playVoice(elementCode);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent: ");
        if (intent != null) {
            elementCode = intent.getIntExtra("value", -1);
        }
        if (elementCode != -1) {
            switch (elementCode) {
                case 1:
                    imageView.setImageResource(R.drawable.egg);
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.rice);
                    break;
                case 3:
                    imageView.setImageResource(R.drawable.bread);
                    break;
                default:
                    break;
            }
        }

        playVoice(elementCode);

    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void playVoice(int code) {
        try {
            if (mediaPlayer == null) {
                switch (code) {
                    case 1:
                        mediaPlayer = MediaPlayer.create(this, R.raw.egg);
                        break;
                    case 2:
                        mediaPlayer = MediaPlayer.create(this, R.raw.rice);
                        break;
                    case 3:
                        mediaPlayer = MediaPlayer.create(this, R.raw.bread);
                        break;
                    default:
                        mediaPlayer = MediaPlayer.create(this, R.raw.egg);
                        break;
                }
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置成可以循环播放
            mediaPlayer.setLooping(false);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    count++;
                    if (count >= 5) {
                        mediaPlayer.stop();
                        count = 0;
                    }
                }
            });
            //mediaPlayer.setVolume(0.01f, 0.01f);
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
    }
}
