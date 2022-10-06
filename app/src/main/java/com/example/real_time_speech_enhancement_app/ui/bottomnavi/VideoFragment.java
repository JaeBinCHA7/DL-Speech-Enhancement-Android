package com.example.real_time_speech_enhancement_app.ui.bottomnavi;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.fragment.app.Fragment;

import com.example.real_time_speech_enhancement_app.R;
import com.example.real_time_speech_enhancement_app.ui.AudioFromVideo;
import com.example.real_time_speech_enhancement_app.ui.FileUtils;
import com.example.real_time_speech_enhancement_app.ui.VideoRecordActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VideoFragment extends Fragment implements View.OnClickListener{

    Intent intent = new Intent();
    private View view;
    Context ct;
    VideoView videoView;
    private static String video = Environment.getExternalStorageDirectory()+"/Download/Sintel_1080p.mp4";
    private static TextView textPath = null;
    AudioFromVideo mAudioFromVideo;
    Uri selectedMediaUri ,selectedcleanMediaUri;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_video, container,false);

        ImageButton btn_video_record_start = (ImageButton)view.findViewById(R.id.btn_video_record_start);
        /*Button btn2 = (Button)view.findViewById(R.id.btn2);
        Button btn3 = (Button)view.findViewById(R.id.btn3);*/
        ImageButton btn_video_upload = (ImageButton)view.findViewById(R.id.btn_video_upload);
        ImageButton btn_play_clean = (ImageButton)view.findViewById(R.id.btn_play_clean);
        ImageButton btn_play_original = (ImageButton)view.findViewById(R.id.btn_play_original);


        btn_video_record_start.setOnClickListener(this);
        btn_video_upload.setOnClickListener(this);
        btn_play_clean.setOnClickListener(this);
        btn_play_original.setOnClickListener(this);


        ct = container.getContext();

        //mActivity = MainActivity.this;


        videoView = view.findViewById(R.id.videoView);


        // Inflate the layout for this fragment

        return view;


    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){


            /*case R.id.btn_video_record_start:
                //Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction("android.intent.action.GET_CONTENT");
                break;*/

            case R.id.btn_play_clean:
                MediaController mc = new MediaController((ct));
                VideoView video = this.videoView;

                //Intrinsics.checkNotNull(var10000);
                video.setMediaController(mc);


                video.setVideoPath("/sdcard/Movies/kakaotalk_1663494122509_1.mp4");
                Log.i(TAG, "비디오: "+ video);

                video.start();
                break;

            case R.id.btn_play_original:
                MediaController mc2 = new MediaController((ct));
                VideoView video2 = this.videoView;

                //Intrinsics.checkNotNull(var10000);
                video2.setMediaController(mc2);
                video2.setVideoPath(String.valueOf(selectedMediaUri));
                Log.i(TAG, "비디오: "+ video2);

                video2.start();
                break;

            case R.id.btn_video_record_start:
                Intent intent3 = new Intent(ct, VideoRecordActivity.class);
                startActivity(intent3);
                break;

            case R.id.btn_video_upload:
                //Intent intent4 = new Intent( );
                intent.setType("video/*");
                intent.setAction("android.intent.action.GET_CONTENT");
                //this.startActivityForResult(intent, 100);
                this.startActivityForResult(intent, 101);

                //btnCompareVideoFile.setEnabled(false);
                break;
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == -1) {
            MediaController mc = new MediaController((ct));
            VideoView video = this.videoView;

            //Intrinsics.checkNotNull(var10000);
            video.setMediaController(mc);

            //Intrinsics.checkNotNull(data);
            selectedMediaUri = data.getData();

            //video = this.videoView;
            //Intrinsics.checkNotNull(var10000);

            video.setVideoPath(String.valueOf(selectedMediaUri));
            Log.i(TAG, "비디오: "+ video);
            //video = this.videoView;
            //Intrinsics.checkNotNull(var10000);
            video.start();

        }

        if (requestCode == 100 && resultCode == -1)
        {
            String audiopath = Environment.getExternalStorageDirectory() + "/testout.pcm";
            Log.i(TAG, "오디오: "+audiopath);
            selectedMediaUri = data.getData();
            //szPath = getRealPathFromURI(mContext, selectedMediaUri);
            video = FileUtils.getPath(ct, selectedMediaUri);
            //video = getRemovableSDCardPath(context).split("/Android")[0];
            Log.i(TAG, "비디오: "+video);
            if (video != null) {


                mAudioFromVideo = new AudioFromVideo(video,audiopath);
                mAudioFromVideo.start();
                try {
                    PlayShortAudioFileViaAudioTrack(audiopath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                textPath.setText("Please select video file!");
            }

        }
    }

    /*Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String audio = Environment.getExternalStorageDirectory() + "/output.pcm";
            try {
                PlayShortAudioFileViaAudioTrack(audio);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    Thread thread = new Thread(runnable);*/

    private void PlayShortAudioFileViaAudioTrack(String filePath) throws IOException
    {
// We keep temporarily filePath globally as we have only two sample sounds now..
        if (filePath==null)
            return;

//Reading the file..
        byte[] byteData = null;
        File file = null;
        file = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
        byteData = new byte[(int) file.length()];
        FileInputStream in = null;
        try {
            in = new FileInputStream( file );
            in.read( byteData );
            in.close();

        } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }

        int sampleRate = 44100;
        // AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode)
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        if (at!=null) {
            at.play();
// Write the byte array to the track
            at.write(byteData, 0, byteData.length);
            at.stop();
            at.release();
        }
        else
            Log.d("TCAudio", "audio track is not initialised ");

    }
}