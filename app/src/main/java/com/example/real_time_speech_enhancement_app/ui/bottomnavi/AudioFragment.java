package com.example.real_time_speech_enhancement_app.ui.bottomnavi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.real_time_speech_enhancement_app.R;
import com.example.real_time_speech_enhancement_app.ui.AudioAdapter;
import com.example.real_time_speech_enhancement_app.ui.FileUtils;
import com.example.real_time_speech_enhancement_app.ui.audioNoiseReduction;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AudioFragment extends Fragment implements View.OnClickListener {

    // 리사이클러뷰
    private AudioAdapter audioAdapter;
    private ArrayList<Uri> audioList;
    private RecyclerView audioRecyclerView;

    // 오디오 권한
    private String recordPermission = Manifest.permission.RECORD_AUDIO;
    private int PERMISSION_CODE = 21;

    // 오디오 녹음 변수
    private String audioFileName; // 오디오 녹음 생성 파일 이름
    private String enhancedFileName; // 오디오 녹음 생성 파일 이름
    private String uploadFileName;
    private Button btn_se;
    private Button test;
    private ImageButton test2;
    private TextView text_record;
    private ImageButton btn_record_start;
    private ImageButton btn_audio_upload;
    private int SAMPLE_RATE = 16000;
    public Thread mRecordThread = null;
    private int rBufferSize =
            AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); // 1280
    public boolean isRecording = false;
    public AudioRecord mAudioRecord = null;
    private static final String TAG = "GuideFragment";
    private Thread recordingThread = null;
    private Uri audioUri = null; // 오디오 파일 uri
    private Uri audioUri2 = null; // 오디오 파일 uri

    // 오디오 재생 변수
    private Boolean isPlaying = false;
    ImageView playIcon;
    public static AudioTrack mAudioTrack;
    private int tBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    public Thread mPlayThread = null;

    // 오디오 로드 변수

    // TFLite 변수
    private int stride = 128;
    private int frame_len = 512;
    private double[] frameBuffer = new double[frame_len];
    private audioNoiseReduction audioNoiseReduction;
//        private String convPath_1 = "nunet_v4.tflite";
    private String convPath_1 = "nunet_v1-2.tflite";

    Context ct;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        initNoiseReduction();
        ct = container.getContext();

        // Get PCM data
        mRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                if (rBufferSize == AudioRecord.ERROR || rBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    rBufferSize = SAMPLE_RATE * 2;
                }

                byte[] readData = new byte[rBufferSize];
                String mFilepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Nozae/data";
                FileOutputStream fos = null;

                try {
                    fos = new FileOutputStream(mFilepath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                while (isRecording) {
                    int ret = mAudioRecord.read(readData, 0, rBufferSize);  // AudioRecord의 read 함수를 통해 pcm data 를 읽어옴
                    Log.d(TAG, "read bytes is " + ret);

                    try {
                        fos.write(readData, 0, rBufferSize); // 읽어온 readData, 파일에 write 함
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                isRecording = false; // 녹음 상태 값

                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;

                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        View v = inflater.inflate(R.layout.fragment_audio, container, false);

        btn_se = v.findViewById(R.id.btn_se);
        btn_record_start = v.findViewById(R.id.btn_record_start); // 녹음 버튼
        btn_audio_upload = v.findViewById(R.id.btn_audio_upload); // 녹음 버튼
        text_record = v.findViewById(R.id.text_record); // 녹음 텍스트
        audioRecyclerView = v.findViewById(R.id.recyclerview); // 녹음 리사이클러

//        btn_se.setOnClickListener(this);
        btn_record_start.setOnClickListener(this);
        btn_audio_upload.setOnClickListener(this);

        // 리사이클러뷰
        audioList = new ArrayList<>();
        audioAdapter = new AudioAdapter(getActivity().getApplicationContext(), audioList);
        audioRecyclerView.setAdapter(audioAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        audioRecyclerView.setLayoutManager(mLayoutManager);

        /*
        * 1. btn_record_play
        * 2. btn_download
        * 3. btn_se
        *
        * 버튼 총 3개는 item_audio.xml의 리스트 목록 안 버튼
         item_audio.xml UI가 리사이클러뷰에 계속해서 추가되는 방식임. 각 리스트의 버튼을 누르면 밑의 onItemClick 기능이 실행.
         btn_se 조건문 마지막 줄에 audio Uri 업데이트가 있는데, 이걸 추가해야 리사이클러뷰에 추가되는 것임.
         * 그럼 오디오 불러오기 기능 프로세스는 (1) 오디오를 저장소로 불러온다. (2) 저장소에 있는 오디오를 라사이클러뷰에 추가한다. 이 정도가 될 듯?
        */
        // 커스텀 이벤트 리스너 4. 액티비티에서 커스텀 리스너 객체 생성 및 전달
        audioAdapter.setOnItemClickListener(new AudioAdapter.OnIconClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                View vi = inflater.inflate(R.layout.item_audio, container, false);
                String clickedPath = String.valueOf(audioList.get(position));
                // /storage/emulated/0/Android/data/com.example.yonsei_isp_se_v3/files/RecordFile_20220908_165608_audio.wav
                String viewName = getContext().getResources().getResourceEntryName(view.getId());

                if (viewName.equals("btn_record_play")) {
                    if (isPlaying) {
                        // 음성 녹화 파일이 여러개를 클릭했을 때 재생중인 파일의 Icon을 비활성화(비 재생중)으로 바꾸기 위함.
                        if (playIcon == (ImageView) view) { // 같은 파일을 클릭했을 경우
                            stopAudio();
                        } else { // 다른 음성 파일을 클릭했을 경우 기존의 재생중인 파일 중지
                            stopAudio();
                            playIcon = (ImageView) view; // 새로 파일 재생하기
                            playAudio(clickedPath);
                        }
                    } else {
                        playIcon = (ImageView) view; // btn_recodr_play
                        playAudio(clickedPath);
                    }
                } else if (viewName.equals("btn_download")) {
                    Toast.makeText(getActivity().getApplicationContext(), "download", Toast.LENGTH_SHORT).show();

                } else if (viewName.equals("btn_se")) {
                    double startTime = System.currentTimeMillis();
                    Toast.makeText(getActivity().getApplicationContext(), "Speech Enhancement", Toast.LENGTH_SHORT).show();
                    String recordPath = getActivity().getExternalFilesDir("/").getAbsolutePath(); // /storage/emulated/0/Android/data/com.example.yonsei_isp_se/files
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    enhancedFileName = recordPath + "aui/" + "EnhancedFile_" + timeStamp + "_" + "audio.pcm";

                    // Enhanced File Write
                    FileOutputStream os2 = null;
                    try {
                        os2 = new FileOutputStream(enhancedFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    byte[] audioByte = loadAudio(clickedPath);
                    short[] audioShort = byteArrayToShortArray(audioByte);
                    double[] audioDouble = short2double(audioShort);
                    // Min-Max Normalization
//                    double[] audioDoubleNorm = minMaxNormm(audioDouble);
                    // Framing
                    double[][][] frames = signalFrame(audioDouble); // [1, 372, 512]

//                    double[] hann_window = obtainHanWindow(frame_len);
//                    hann_window[0] = 1e-7;
//                    hann_window[511] = 1e-7;
                    // Windowing
//                    double[][][] window_frames = hanningWindow(frames, hann_window);

                    float[][][] mags = new float[1][frames[0].length][257];
                    float[][][] phases = new float[1][frames[0].length][257];
                    Complex[] realTranformedBuffer;
                    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

                    for (int i = 0; i < frames[0].length; i++) {
                        frameBuffer = frames[0][i];
                        // FFT
                        Complex[] transformedBuffer = fft.transform(frameBuffer, TransformType.FORWARD);
                        realTranformedBuffer = getComplexSlice(transformedBuffer, 0, 257);
                        // get magnitude/phase
                        for (int j = 0; j < realTranformedBuffer.length; j++) {
                            double real = realTranformedBuffer[j].getReal();
                            double imag = realTranformedBuffer[j].getImaginary();
                            mags[0][i][j] = (float) Math.sqrt(Math.pow(real, 2) + Math.pow(imag, 2));
                            phases[0][i][j] = (float) Math.atan2(imag, real);
                        } // [257]
                    }

                    // Reshape [1,372,257] -> [1,372,256,1]
                    float[][][][] remags = reshapeBeforeEncoder(mags);

                    float[][][][] tflite_out = audioNoiseReduction.runningTFLite_1(remags);

//                    for (int i = 0; i < tflite_out[0].length; i++) {
//                        for (int j = 0; j < tflite_out[0][0].length; j++) {
//                            tflite_out[0][i][j][0] = tflite_out[0][i][j][0] * (float) hann_window[j];
//                        }
//                    }
                    // Reshape and Padding [1,372,256,1] -> [1,372,257]
                    float[][][] out_mask = reshapeAndPadding(tflite_out);

                    // TF-Masking
                    double[][][] outReal = getReal(mags, phases, out_mask);
                    double[][][] outImag = getImag(mags, phases, out_mask);

                    // IFFT
                    Complex[][][] outComplex = createComplex(outReal, outImag);
                    Complex[][][] fullComplex = reconstructComplex(outComplex);

                    FastFourierTransformer ifft = new FastFourierTransformer(DftNormalization.STANDARD);
                    double[][][] frame_audio = new double[1][fullComplex[0].length][fullComplex[0][0].length];
                    for (int i = 0; i < fullComplex[0].length; i++) {
                        Complex[] ifft_frame = fullComplex[0][i];
                        Complex[] detransBuffer = ifft.transform(ifft_frame, TransformType.INVERSE);
                        // Get real
                        for (int j = 0; j < detransBuffer.length; j++) {
                            frame_audio[0][i][j] = detransBuffer[j].getReal();
                        }
                    }

                    // Windowing
//                    double[][][] window_frames2 = hanningWindow(frame_audio, hann_window);

                    // Denormalization
//                    double[][][] denorm_frames = deNormalization(window_frames2, hann_window);

                    // Out-buffer : Overlap-add
                    double[] outAudio = overlapAdd(frame_audio, audioDouble.length);

                    short[] sAudio = doubleArrayToShortArray(outAudio);

                    byte[] recoredAudio = short2byte(sAudio);

                    try {
                        os2.write(recoredAudio, 0, recoredAudio.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    double endTime = System.currentTimeMillis();
                    System.out.println("1 loop's processing time : " + (endTime - startTime) / 1000 + "s\n");
                    double[] loss = new double[audioDouble.length];
                    for (int i = 0; i < audioDouble.length; i++) {
                        loss[i] = audioDouble[i] - outAudio[i];
                    }
                    double sum = 0;
                    for (int i = 0; i < loss.length; i++) {
                        loss[i] = Math.pow(loss[i], 2);
                        sum += loss[i];
                    }
                    double avg = sum / loss.length;
                    System.out.println("Loss : " + avg);

                    // 리사이클러뷰 추가
                    audioUri2 = Uri.parse(enhancedFileName);
                    audioList.add(audioUri2);
                    audioAdapter.notifyDataSetChanged();
                }
            }
        });

        return v;
    }


    // 녹음 파일 재생
    private void playAudio(String clickedURL) {
        // 스트림 타입 / Sampling rate / 채널 / 오디오 포맷 / 버퍼 사이즈
        mAudioTrack =
                new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        tBufferSize,
                        AudioTrack.MODE_STREAM); // AudioTrack 생성

        mPlayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] writeData = new byte[tBufferSize];
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(clickedURL);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                DataInputStream dis = new DataInputStream(fis);
                mAudioTrack.play();  // write 하기 전에 play 를 먼저 수행해 주어야 함

                while (isPlaying) {
                    try {
                        int ret = dis.read(writeData, 0, tBufferSize); // return Data length
                        if (ret <= 0) {
                            getActivity().runOnUiThread(new Runnable() { // UI 컨트롤을 위해
                                @Override
                                public void run() {
                                    isPlaying = false;
                                }
                            });
                            break;
                        }
                        mAudioTrack.write(writeData, 0, ret); // AudioTrack 에 write 를 하면 스피커로 송출됨
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;

                try {
                    dis.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mPlayThread.start();
        playIcon.setImageDrawable(getResources().getDrawable(R.drawable.btn_pause, null));
        isPlaying = true;

//        mAudioTrack.setNotificationMarkerPosition(input_lite.length);
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack audioTrack) {
                stopAudio();
            }

            @Override
            public void onPeriodicNotification(AudioTrack audioTrack) {
                // nothing to do
            }
        });
    }

    // 녹음 재생 중
    private void stopAudio() {
        playIcon.setImageDrawable(getResources().getDrawable(R.drawable.btn_play, null));
        isPlaying = false;
//        mediaPlayer.stop();
        mAudioTrack.stop();

    }

    // 오디오 파일 권한 체크
    private boolean checkAudioPermission() {
        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), recordPermission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{recordPermission}, PERMISSION_CODE);
            return false;
        }
    }

    // 녹음 시작
    private void startRecording() {
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        // 파일의 외부 경로 확인
        String recordPath = getActivity().getExternalFilesDir("/").getAbsolutePath(); // /storage/emulated/0/Android/data/com.example.yonsei_isp_se/files
        // 파일 이름 변수를 현재 날짜가 들어가도록 초기화. 그 이유는 중복된 이름으로 기존에 있던 파일이 덮어 쓰여지는 것을 방지하고자 함.
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        audioFileName = recordPath + "/" + "RecordFile_" + timeStamp + "_" + "audio.pcm";

        short sData[] = new short[stride];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            mAudioRecord.read(sData, 0, sData.length);
            try {
                // Writes the recored audio to file
                byte bData[] = short2byte(sData);
                os.write(bData, 0, bData.length);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        byte[] bytes = new byte[sData.length * 2];
        for (int i = 0; i < sData.length; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        }
        return bytes;
    }

    private double[] short2double(short[] shortData) {
        int size = shortData.length;
        double[] doubleData = new double[size];
        for (int i = 0; i < size; i++) {
            doubleData[i] = shortData[i] / 32768.0;
        }
        return doubleData;
    }

    public static short[] byteArrayToShortArray(byte[] byteArray) {
        int nlengthInSamples = byteArray.length / 2;
        short[] audioData = new short[nlengthInSamples];

        for (int i = 0; i < nlengthInSamples; i++) {
            short MSB = (short) byteArray[2 * i + 1];
            short LSB = (short) byteArray[2 * i];
            audioData[i] = (short) (MSB << 8 | (255 & LSB));
        }

        return audioData;
    }

    // Reshape and Padding [1,1,256,1] -> [1,1,257]
    private float[][][] reshapeAndPadding(float[][][][] input) {
        int frame_num = input[0].length;
        float[][][] matrix = new float[1][frame_num][257];
        matrix[0][0][0] = 0;
        for (int i = 0; i < frame_num; i++) {
            for (int j = 1; j < 257; j++) {
                matrix[0][i][j] = input[0][i][j - 1][0];
            }
        }

        return matrix;
    }

    private double[][][] getReal(float[][][] mag, float[][][] phase, float[][][] mask) {
        int frame_num = mag[0].length;
        int fft_bins = mag[0][0].length;

        float[][][] enhancedMag = new float[1][frame_num][fft_bins];
        double[][][] real = new double[1][frame_num][fft_bins];

        for (int i = 0; i < frame_num; i++) {
            for (int j = 0; j < fft_bins; j++) {
                mask[0][i][j] = (float) Math.tanh(mask[0][i][j]);
                enhancedMag[0][i][j] = mag[0][i][j] * mask[0][i][j];
                real[0][i][j] = enhancedMag[0][i][j] * (float) Math.cos(phase[0][i][j]);
            }
        }

        return real;
    }

    private double[][][] getImag(float[][][] mag, float[][][] phase, float[][][] mask) {
        int frame_num = mag[0].length;
        int fft_bins = mag[0][0].length;

        float[][][] enhancedMag = new float[1][frame_num][fft_bins];
        double[][][] imag = new double[1][frame_num][fft_bins];

        for (int i = 0; i < frame_num; i++) {
            for (int j = 0; j < fft_bins; j++) {
                mask[0][i][j] = (float) Math.tanh(mask[0][i][j]);
                enhancedMag[0][i][j] = mag[0][i][j] * mask[0][i][j];
                imag[0][i][j] = enhancedMag[0][i][j] * (float) Math.sin(phase[0][i][j]);
            }
        }

        return imag;
    }

    private Complex[][][] createComplex(double[][][] real, double[][][] imag) {
        int frame_num = real[0].length;
        int fft_bins = real[0][0].length;

        Complex[][][] complexArray = new Complex[1][frame_num][fft_bins];

        for (int i = 0; i < frame_num; i++) {
            for (int j = 0; j < fft_bins; j++) {
                complexArray[0][i][j] = new Complex(real[0][i][j], imag[0][i][j]);
            }
        }

        return complexArray;
    }

    private Complex[][][] reconstructComplex(Complex[][][] half) { // 257
        int frame_num = half[0].length;
        int fft_bins = half[0][0].length; // 257

        Complex[][][] fullComplex = new Complex[1][frame_num][(fft_bins - 1) * 2]; // 512
        Complex[][][] conjugateComplex = new Complex[1][frame_num][fft_bins - 1];

        for (int i = 0; i < frame_num; i++) { // 0~256
            for (int j = 0; j < fft_bins; j++) {
                fullComplex[0][i][j] = half[0][i][j];
            }
        }
        for (int i = 0; i < frame_num; i++) {
            for (int j = 1; j < fft_bins - 1; j++) { // 1~255
                conjugateComplex[0][i][j] = half[0][i][j].conjugate();
                fullComplex[0][i][fullComplex[0][0].length - j] = conjugateComplex[0][i][j];
            }
        }
        return fullComplex;
    }

    private double[] overlapAdd(double[][][] frames, int audio_len) {
        int frame_num = frames[0].length;
        int frame_len = frames[0][0].length;

        double[] ola = new double[audio_len];

        for (int i = 0; i < frame_num; i++) {
            for (int j = 0; j < frame_len; j++) {
                ola[i * stride + j] += frames[0][i][j];
            }
        }

        return ola;
    }

    private double[][][] hanningWindow(double[][][] frames, double[] window) {
        int frame_num = frames[0].length;
        int window_len = frames[0][0].length;
        double[] frame = new double[window_len];

        for (int i = 1; i < frame_num; i++) {
//            for (int j = 0; j < window_len; j++) {
////                frames[0][i][j] = frames[0][i][j] * window[j];
//                frames[0][i][j] = frames[0][i][j];
//
//            }
            for(int j=0;j<window_len;j++){
                frame[j] = frames[0][i][j];
            }
            frames[0][i] = applyingWindow(window, frame);
        }

        return frames;
    }

    private double[] applyingWindow(double[] window, double[] dSamples) {
        double[] dOutput = new double[dSamples.length];
        for (int i = 0; i < window.length; i++) {
            dOutput[i] = 0;
            for (int j = 0; j < dSamples.length; j++) {
                if (i - j >= 0) {
                    dOutput[i] += dSamples[j] * window[i - j];
                }
            }
        }
        return dOutput;
    }

    private double[] obtainHanWindow(int frame_len) {
        //Size must be Odd and Positive integer
        double[] dHan = new double[frame_len];
        int HalfSize = (int) Math.floor((double) frame_len / 2);
        for (int i = 0; i < HalfSize; i++) {
            int j = 2 * HalfSize - i - 1;
            double Pi2DivSize = 2.0 * Math.PI / frame_len;
            dHan[i] = 0.5 + 0.5 * Math.cos(Pi2DivSize * (double) (i - HalfSize));
            dHan[j] = 0.5 + 0.5 * Math.cos(Pi2DivSize * (double) (HalfSize - i));
        }
        dHan[HalfSize] = 1.0;
        return dHan;
    }

    private double[][][] deNormalization(double[][][] frames, double[] window) {
        int frame_num = frames[0].length;
        int window_len = frames[0][0].length;
        double[] square_window = new double[window.length];
        for (int k = 0; k < window.length; k++) {
            square_window[k] = 4 * Math.pow(window[k], 2);
        }

        for (int i = 1; i < frame_num; i++) {
            for (int j = 0; j < window_len; j++) {
                frames[0][i][j] = frames[0][i][j] / square_window[j];
            }
        }

        return frames;
    }

    private double[] shiftLeft(double[] input, int stride) {
        for (int i = 0; i < (frame_len - stride); i++) {
            input[i] = input[stride + i];
        }
        for (int i = (frame_len - stride); i < frame_len; i++) {
            input[i] = 0;
        }
        return input;
    }

    private double[] concatDoubleArray(double[] array1, double[] array2) {
        int lenArray1 = array1.length;
        int lenArray2 = array2.length;

        double[] concatArray = new double[lenArray1 + lenArray2];

        System.arraycopy(array1, 0, concatArray, 0, lenArray1);
        System.arraycopy(array2, 0, concatArray, lenArray1, lenArray2);

        return concatArray;
    }

    private short[] doubleArrayToShortArray(double[] doubleArray) {
        short[] shortArray = new short[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            shortArray[i] = (short) (doubleArray[i] * 32768.0);
        }
        return shortArray;
    }

    public short[] getSlice(short[] array, int startIndex, int endIndex) {
        // Get the slice of the Array
        short[] slicedArray = new short[endIndex - startIndex];
        //copying array elements from the original array to the newly created sliced array
        for (int i = 0; i < slicedArray.length; i++) {
            slicedArray[i] = array[startIndex + i];
        }
        //returns the slice of an array
        return slicedArray;
    }

    public double[] getSliceDoubleArray(double[] array, int startIndex, int endIndex) {
        // Get the slice of the Array
        double[] slicedArray = new double[endIndex - startIndex];
        //copying array elements from the original array to the newly created sliced array
        for (int i = 0; i < slicedArray.length; i++) {
            slicedArray[i] = array[startIndex + i];
        }
        //returns the slice of an array
        return slicedArray;
    }

    // 녹음 종료
    private void stopRecording() {
        // stops the recording activity
        if (null != mAudioRecord) {
            isRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
            recordingThread = null;
        }

        // 파일 경로(String) 값을 Uri로 변환해서 저장 -> 맞는듯
        //      - Why? : 리사이클러뷰에 들어가는 ArrayList가 Uri를 가지기 때문
        //      - File Path를 알면 File을  인스턴스를 만들어 사용할 수 있기 때문
        audioUri = Uri.parse(audioFileName);

        // 데이터 ArrayList에 담기
        audioList.add(audioUri);

        // 데이터 갱신
        audioAdapter.notifyDataSetChanged();

    }

    void initNoiseReduction() {
        try {
            audioNoiseReduction = new audioNoiseReduction(getActivity(), convPath_1);
        } catch (IOException e) {
            Log.d("class", "Failed to create noise reduction");
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record_start:
                if (isRecording) {
                    // 현재 녹음 중 O
                    // 녹음 상태에 따른 변수 아이콘 & 텍스트 변경
                    btn_record_start.setImageDrawable(getResources().getDrawable(R.drawable.img_audio_record, null)); // 녹음 상태 아이콘 변경
//                    text_record.setText("녹음 시작"); // 녹음 상태 텍스트 변경

                    stopRecording();
                    // 녹화 이미지 버튼 변경 및 리코딩 상태 변수값 변경

                } else {
                    // 현재 녹음 중 X
                    /**절차
                     *       1. Audio 권한 체크
                     *       2. 처음으로 녹음 실행한건지 여부 확인
                     * */
                    if (checkAudioPermission()) {
                        // 녹음 상태에 따른 변수 아이콘 & 텍스트 변경
                        isRecording = true; // 녹음 상태 값
                        btn_record_start.setImageDrawable(getResources().getDrawable(R.drawable.img_audio_recording, null)); // 녹음 상태 아이콘 변경
//                        text_record.setText("녹음 중"); // 녹음 상태 텍스트 변경

                        // AudioRecord 생성 및 초기화
                        mAudioRecord =
                                new AudioRecord(
                                        MediaRecorder.AudioSource.DEFAULT,
                                        SAMPLE_RATE,
                                        AudioFormat.CHANNEL_IN_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        rBufferSize);

                        mAudioRecord.startRecording();
                        startRecording();
                        Toast.makeText(getActivity().getApplicationContext(), "녹음이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.btn_audio_upload:
                Intent intent = new Intent();
                intent.setType("audio/*");
                intent.setAction("android.intent.action.GET_CONTENT");

//                intent.setAction(Intent.ACTION_VIEW);
//
//                String recordPath = getActivity().getExternalFilesDir("/").getAbsolutePath(); // /storage/emulated/0/Android/data/com.example.yonsei_isp_se/files
//                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                uploadFileName = recordPath + "/" + "UploadedFile_" + timeStamp + "_" + "audio.pcm";
//                File audioFile = new File(uploadFileName);
//                Uri uriFromAudioFile = Uri.fromFile(audioFile);
//                intent.setDataAndType(uriFromAudioFile, "audio/*");

                this.startActivityForResult(intent, 101);
                break;
        }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == -1)
        {
            Uri selectedMediaUri = data.getData();
//            String filePath = selectedMediaUri.getPath();
            String audioPath = FileUtils.getPath(ct, selectedMediaUri);
            audioUri2 = Uri.parse(audioPath);
            audioList.add(audioUri2);
            audioAdapter.notifyDataSetChanged();


        }
    }

    private double[][][] signalFrame(double[] signal) {
        int frame_len = 512;
        int stride = 128;
        int frame_number = (signal.length - (frame_len - stride)) / stride;

        double[][][] frames = new double[1][frame_number][frame_len];
        for (int i = 0; i < frame_number; i++) {
            for (int j = 0; j < frame_len; j++) {
                frames[0][i][j] = signal[stride * i + j];
            }
        }

        return frames;
    }
//
//    private double[][][] signalFrame(double[] signal) {
//        int frame_len = 512;
//        int stride = 128;
////        int frame_number = (signal.length - (frame_len - stride)) / stride;
//        int frame_number = signal.length / stride;
//
//        // signal 마지막에 stride * 3 만큼 패딩
//        double[] paddingSignal = new double[signal.length + stride * 3];
//
//        for (int i = 0; i < signal.length; i++) {
//            paddingSignal[i] = signal[i];
//        }
//
//        double[][][] frames = new double[1][frame_number][frame_len];
//        for (int i = 0; i < frame_number; i++) {
//            for (int j = 0; j < frame_len; j++) {
//                frames[0][i][j] = paddingSignal[stride * i + j];
//            }
//        }
//
//        return frames;
//    }


    private byte[] loadAudio(String clickedURL) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(clickedURL);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        DataInputStream dis = new DataInputStream(fis);
        int length = 0;
        try {
            length = fis.available();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
//        byte[] writeData = new byte[tBufferSize];
        byte[] writeData = new byte[length];

        try {
            int ret = dis.read(writeData);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return writeData;
    }


    public Complex[] getComplexSlice(Complex[] array, int startIndex, int endIndex) {
        // Get the slice of the Array
        Complex[] slicedArray = new Complex[endIndex - startIndex];
        //copying array elements from the original array to the newly created sliced array
        for (int i = 0; i < slicedArray.length; i++) {
            slicedArray[i] = array[startIndex + i];
        }
        //returns the slice of an array
        return slicedArray;
    }

    // Reshape [1,372,257] -> [1,372,256,1]
    private float[][][][] reshapeBeforeEncoder(float[][][] input) {
        int frame_num = input[0].length;
        int fft_bins = input[0][0].length - 1;
        float[][][][] matrix = new float[1][frame_num][fft_bins][1];
        for (int i = 0; i < frame_num; i++) {
            for (int j = 0; j < fft_bins; j++) {
                matrix[0][i][j][0] = input[0][i][j + 1];
            }
        }
        return matrix;
    }

    // Method to find minimum value from the data set
    private double minValue(double arr[]) {
        double min = arr[0];
        for (int i = 1; i < arr.length; i++)
            if (arr[i] < min) min = arr[i];
        return min;
    }

    // Method to find maximum value from the data set
    private double maxValue(double arr[]) {
        double max = arr[0];
        for (int i = 1; i < arr.length; i++)
            if (arr[i] > max) max = arr[i];
        return max;
    }

    // Method to find the normalized values of the data set
    private double[] minMaxNormm(double arr[]) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (arr[i] - minValue(arr) / (maxValue(arr) - minValue(arr) + 1e-8));
        }

        return arr;
    }

}