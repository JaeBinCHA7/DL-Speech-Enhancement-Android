package com.example.real_time_speech_enhancement_app.ui.bottomnavi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import com.example.real_time_speech_enhancement_app.ui.NoiseReduction;

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

//import com.example.real_time_speech_enhancement_app.ui.Complex;

public class StartFragment extends Fragment implements View.OnClickListener {

    private Button btn_se, btn_play, btn_pause, btn_stop;
    private TextView text_record;
    private ImageButton btn_record_start;
    private RecyclerView audioRecyclerView;
    private int pauseposition;

    /**
     * 오디오 파일 관련 변수
     */
    // 오디오 권한
    private String recordPermission = Manifest.permission.RECORD_AUDIO;
    private int PERMISSION_CODE = 21;

    // 오디오 파일 녹음 관련 변수
    private String audioFileName; // 오디오 녹음 생성 파일 이름
    private String enhancedFileName; // 오디오 녹음 생성 파일 이름

    private Uri audioUri = null; // 오디오 파일 uri
    private Uri audioUri2 = null; // 오디오 파일 uri

    private int SAMPLE_RATE = 16000;
    private int rBufferSize =
            AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); // 1280
    private int tBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    public AudioRecord mAudioRecord = null;
    public Thread mRecordThread = null;
    public boolean isRecording = false;
    private static final String TAG = "StartFragment";
    private Thread recordingThread = null;

    // 오디오 파일 재생 관련 변수
    private MediaPlayer mediaPlayer = null;
    private Boolean isPlaying = false;
    private Boolean isDownloading = false;
    ImageView playIcon;
    public Thread mPlayThread = null;
    public static AudioTrack mAudioTrack;

    // 오디오 다운로드 관련 변수
    private MediaStore mediaStore = null;

    // 리사이클러뷰
    private AudioAdapter audioAdapter;
    private ArrayList<Uri> audioList;

    // TFLite 변수
    private NoiseReduction noiseReduction;
    private int frame_len = 512;
    private int stride = 128;
    private double[] in_buffer = new double[frame_len];
    private double[] out_buffer = new double[frame_len];

//    private String modelPath_1 = "encoder-noskip-kernel1-E30.tflite";
//    private String modelPath_2 = "lstm-noskip-kernel1-E30.tflite";
//    private String modelPath_3 = "decoder-noskip-kernel1-E30.tflite";

    private String convPath_1 = "conv1.tflite";
    private String convPath_2 = "conv2.tflite";
    private String convPath_3 = "conv3.tflite";
    private String convPath_4 = "conv4.tflite";
    private String convPath_5 = "conv5.tflite";
    private String convPath_6 = "conv6.tflite";
    private String lstmPath = "lstm.tflite";
    private String convTPath_1 = "conv_t1.tflite";
    private String convTPath_2 = "conv_t2.tflite";
    private String convTPath_3 = "conv_t3.tflite";
    private String convTPath_4 = "conv_t4.tflite";
    private String convTPath_5 = "conv_t5.tflite";
    private String convTPath_6 = "conv_t6.tflite";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initNoiseReduction();

        // Get PCM data
        mRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

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

        View v = inflater.inflate(R.layout.fragment_start, container, false);
        btn_record_start = v.findViewById(R.id.btn_record_start);

        btn_se = v.findViewById(R.id.btn_se);
        btn_play = v.findViewById(R.id.btn_play);   // 재생 버튼
        btn_pause = v.findViewById(R.id.btn_pause); // 일시 정지
        btn_stop = v.findViewById(R.id.btn_stop);   // 정지
        btn_record_start = v.findViewById(R.id.btn_record_start); // 녹음 버튼
        text_record = v.findViewById(R.id.text_record); // 녹음 텍스트
        audioRecyclerView = v.findViewById(R.id.recyclerview); // 녹음 리사이클러

        btn_record_start.setOnClickListener(this);
        btn_se.setOnClickListener(this);
        btn_play.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_stop.setOnClickListener(this);


        // 리사이클러뷰
        audioList = new ArrayList<>();
        audioAdapter = new AudioAdapter(getActivity().getApplicationContext(), audioList);
        audioRecyclerView.setAdapter(audioAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        audioRecyclerView.setLayoutManager(mLayoutManager);

        // 커스텀 이벤트 리스너 4. 액티비티에서 커스텀 리스너 객체 생성 및 전달
        audioAdapter.setOnItemClickListener(new AudioAdapter.OnIconClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                String clickedPath = String.valueOf(audioList.get(position));

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
                    playIcon = (ImageView) view;
                    playAudio(clickedPath);
                }

                if (isDownloading){

                }
            }
        });

        // Inflate the layout for this fragment
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

    // 녹음 파일 중지
    private void stopAudio() {
        playIcon.setImageDrawable(getResources().getDrawable(R.drawable.btn_play, null));
        isPlaying = false;
//        mediaPlayer.stop();
        mAudioTrack.stop();

    }

    private void downloadAudio(String clickedURL){
        isDownloading = true;
    }

    void initNoiseReduction() {
        try {
            noiseReduction = new NoiseReduction(getActivity(), convPath_1, convPath_2, convPath_3, convPath_4, convPath_5, convPath_6,
                    lstmPath, convTPath_1, convTPath_2, convTPath_3, convTPath_4, convTPath_5, convTPath_6);
        } catch (IOException e) {
            Log.d("class", "Failed to create noise reduction");
        }

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
        audioFileName = recordPath + "/" + "RecordFile_" + timeStamp + "_" + "audio.wav";
        // Enhanced File Path
        enhancedFileName = recordPath + "/" + "EnhancedFile_" + timeStamp + "_" + "audio.wav";

        short sData[] = new short[stride];
        FileOutputStream os = null;
        Complex[] realTranformedBuffer = new Complex[frame_len / 2];
        try {
            os = new FileOutputStream(audioFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Enhanced File Write
        FileOutputStream os2 = null;
        try {
            os2 = new FileOutputStream(enhancedFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        float[][][][] conv1_buffer = new float[1][2][256][1];
        float[][][][] conv2_buffer = new float[1][2][128][16];
        float[][][][] conv3_buffer = new float[1][2][64][32];
        float[][][][] conv4_buffer = new float[1][2][32][64];
        float[][][][] conv5_buffer = new float[1][2][16][128];
        float[][][][] conv6_buffer = new float[1][2][8][128];
        float[][][] state = new float[1][128][2];

        while (isRecording) {
            // gets the voice output from microphone to byte format
            mAudioRecord.read(sData, 0, sData.length);
            try {
                // Writes the recored audio to file
                byte bData[] = short2byte(sData);
                os.write(bData, 0, bData.length);

                double startTime = System.currentTimeMillis();

                // To FFT, convert to double type
                double[] dData = short2double(sData);

                // Shift left and Input audio
                in_buffer = shiftLeft(in_buffer, stride);
                in_buffer = audioToBuffer(in_buffer, dData);

                // FFT
                FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
                Complex[] transformedBuffer = fft.transform(in_buffer, TransformType.FORWARD);

                // Slicing Complex - Index 256 기준으로 Magnitude는 대칭, Phase +/- 관계를 가짐.
                realTranformedBuffer = getComplexSlice(transformedBuffer, 0, 257);

                // get magnitude/phase
                float[] mags = new float[realTranformedBuffer.length];
                float[] phase = new float[realTranformedBuffer.length];
                for (int i = 0; i < realTranformedBuffer.length; i++) {
                    double real = realTranformedBuffer[i].getReal();
                    double imag = realTranformedBuffer[i].getImaginary();
                    mags[i] = (float) Math.sqrt(Math.pow(real, 2) + Math.pow(imag, 2));
                    phase[i] = (float) Math.atan2(imag, real);
                } // [257]

                // Reshape [257] -> [1,1,256,1]
                float[][][][] remags = reshapeBeforeEncoder(mags);

                // Input mags to TFLite model 1 (Convolution Layer 1)
//                double startEncoderTime = System.currentTimeMillis();
                conv1_buffer = shiftFrameAndAllocateData(conv1_buffer, remags);
                float[][][][] conv1_in = paddingBuffer(conv1_buffer, 3);
                float[][][][] conv1_out = noiseReduction.runningTFLite_1(conv1_in);

                // TFLite model 2 (Convolution Layer 2)
                conv2_buffer = shiftFrameAndAllocateData(conv2_buffer, conv1_out);
                float[][][][] conv2_in = paddingBuffer(conv2_buffer, 3);
                float[][][][] conv2_out = noiseReduction.runningTFLite_2(conv2_in);

                // TFLite model 3 (Convolution Layer 3)
                conv3_buffer = shiftFrameAndAllocateData(conv3_buffer, conv2_out);
                float[][][][] conv3_in = paddingBuffer(conv3_buffer, 3);
                float[][][][] conv3_out = noiseReduction.runningTFLite_3(conv3_in);

                // TFLite model 4 (Convolution Layer 4)
                conv4_buffer = shiftFrameAndAllocateData(conv4_buffer, conv3_out);
                float[][][][] conv4_in = paddingBuffer(conv4_buffer, 3);
                float[][][][] conv4_out = noiseReduction.runningTFLite_4(conv4_in);

                // TFLite model 5 (Convolution Layer 5)
                conv5_buffer = shiftFrameAndAllocateData(conv5_buffer, conv4_out);
                float[][][][] conv5_in = paddingBuffer(conv5_buffer, 3);
                float[][][][] conv5_out = noiseReduction.runningTFLite_5(conv5_in);

                // TFLite model 6 (Convolution Layer 6)
                conv6_buffer = shiftFrameAndAllocateData(conv6_buffer, conv5_out);
                float[][][][] conv6_in = paddingBuffer(conv6_buffer, 3);
                float[][][][] conv6_out = noiseReduction.runningTFLite_6(conv6_in);

//                double endEncoderTime = System.currentTimeMillis();
//                System.out.println("Encoder processing time : " + (endEncoderTime - startEncoderTime) / 1000.0 + "s\n");

                // Reshape [1,1,4,128] -> [1,1,512]
                float[][][] lstm_in = reshapeBeforeLSTM(conv6_out);

                // TFLite model 7 (LSTM)
//                double startLSTMTime = System.currentTimeMillis();
                float[][][][] tflite7_out = noiseReduction.runningTFLite_7(lstm_in, state);
                float[][][] lstm_out = tflite7_out[0];
                state = tflite7_out[1];
//                double endLSTMTime = System.currentTimeMillis();
//                System.out.println("LSTM processing time : " + (endLSTMTime - startLSTMTime) / 1000.0 + "s\n");

                // Reshape [1,1,512] -> [1,1,4,128]
                float[][][][] encoder_in = reshapeBeforeDecoder(lstm_out, conv6_out[0][0].length, conv6_out[0][0][0].length);

                // TFLite model 8 (Convolution Transpose Layer 1)
//                double startDecoderTime = System.currentTimeMillis();
                float[][][][] convT1_in = skipConnection(encoder_in, conv6_out); // Skip Connection
                float[][][][] convT1_out = noiseReduction.runningTFLite_8(convT1_in); // [1,2,32,64]

                // TFLite model 9 (Convolution Transpose Layer 2)
                float[][][][] convT2_in = skipConnection(convT1_out, conv5_out); // Skip Connection
                float[][][][] convT2_out = noiseReduction.runningTFLite_9(convT2_in); // [1,2,64,32]

                // TFLite model 10 (Convolution Transpose Layer 3)
                float[][][][] convT3_in = skipConnection(convT2_out, conv4_out); // Skip Connection
                float[][][][] convT3_out = noiseReduction.runningTFLite_10(convT3_in); // [1,2,128,16]

                // TFLite model 11 (Convolution Transpose Layer 4)
                float[][][][] convT4_in = skipConnection(convT3_out, conv3_out); // Skip Connection
                float[][][][] convT4_out = noiseReduction.runningTFLite_11(convT4_in); // [1,2,128,16]

                // TFLite model 12 (Convolution Transpose Layer 5)
                float[][][][] convT5_in = skipConnection(convT4_out, conv2_out); // Skip Connection
                float[][][][] convT5_out = noiseReduction.runningTFLite_12(convT5_in); // [1,2,128,16]

                // TFLite model 13 (Convolution Transpose Layer 6)
                float[][][][] convT6_in = skipConnection(convT5_out, conv1_out); // Skip Connection
                float[][][][] convT6_out = noiseReduction.runningTFLite_13(convT6_in); // [1,2,128,16]
//                double endDecoderTime = System.currentTimeMillis();
//                System.out.println("Decoder processing time : " + (endDecoderTime - startDecoderTime) / 1000.0 + "s\n");

                // Reshape and Padding [1,1,256,1] -> [1,1,257]
                float[][][] out_mask = reshapeAndPadding(convT6_out);

                // Reshape [1,1,257] -> [257]
                float[] maskOneDim = reshapeThreeToOne(out_mask);

                // TF-Masking
                double[] outReal = getReal(mags, phase, maskOneDim);
                double[] outImag = getImag(mags, phase, maskOneDim);

                // IFFT
                Complex[] outComplex = createComplex(outReal, outImag);
                Complex[] fullComplex = reconstructComplex(outComplex);
                FastFourierTransformer ifft = new FastFourierTransformer(DftNormalization.STANDARD);
                Complex[] detransBuffer = ifft.transform(fullComplex, TransformType.INVERSE);

                // Get real
                double[] real = new double[detransBuffer.length];
                for (int i = 0; i < detransBuffer.length; i++) {
                    real[i] = detransBuffer[i].getReal();
                }
                // Out-buffer : Shift Left and Input zero
                out_buffer = shiftLeft(out_buffer, stride);

                // Out-buffer : Overlap-add
                out_buffer = overlapAdd(out_buffer, real);

                // Convert double array to short array
                short[] sAudio = doubleArrayToShortArray(out_buffer);

                // Slicing out-buffer
                sAudio = getSlice(sAudio, 0, stride);

                byte[] recoredAudio = short2byte(sAudio);
                os2.write(recoredAudio, 0, recoredAudio.length);

                double endTime = System.currentTimeMillis();
                System.out.println("1 loop's processing time : " + (endTime - startTime) / 1000 + "s\n");

//                noiseReduction.closeTFLite();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
            os2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double[] overlapAdd(double[] buffer, double[] audio) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = buffer[i] + audio[i];
        }

        return buffer;
    }

    private float[][][][] shiftFrameAndAllocateData(float[][][][] buffer, float[][][][] data){
        int frameNumber = buffer[0].length;
        int frequencyNumber = buffer[0][0].length;
        int channelNumber = buffer[0][0][0].length;
        float[][][][] newBuffer = new float[1][frameNumber][frequencyNumber][channelNumber];
//        for(int j=0;j<frequencyNumber;j++){
//            for(int k=0;k<channelNumber;k++){
//                newBuffer[0][0][j][k] = buffer[0][1][j][k];
//                newBuffer[0][1][j][k] = data[0][0][j][k];
//            }
//        }
        newBuffer[0][0] = buffer[0][1];
        newBuffer[0][1] = data[0][0];


        return newBuffer;
    }

    private float[][][][] paddingBuffer(float[][][][] buffer, int padding) {
        int frameNumber = buffer[0].length;
        int N = buffer[0][0].length;
        int M = buffer[0][0][0].length;
        float[][][][] paddingBuffer = new float[1][frameNumber][N + padding][M];

        // padding on front
        for (int j = 0; j < N; j++) {
            for (int k = 0; k < M; k++) {
                paddingBuffer[0][0][j + padding][k] = buffer[0][0][j][k];
                paddingBuffer[0][1][j + padding][k] = buffer[0][1][j][k];
            }
        }

        return paddingBuffer;
    }

    private float[][][][] skipConnection(float[][][][] convTout, float[][][][] convOut) {
        int N = convTout[0][0].length;
        int M = convTout[0][0][0].length;
        float[][][][] concatMatrix = new float[1][1][N][M * 2];

        // Concatenate PreFrame and CurFrame in axis = 1
        for (int j = 0; j < N; j++) {
            for (int k = 0; k < M; k++) {
                concatMatrix[0][0][j][k] = convTout[0][0][j][k];
                concatMatrix[0][0][j][k + M] = convOut[0][0][j][k];
            }
        }

        return concatMatrix;
    }


    private Complex[] reconstructComplex(Complex[] half) { // 257
        Complex[] fullComplex = new Complex[(half.length - 1) * 2]; // 512
        Complex[] conjugateComplex = new Complex[half.length - 1]; // 256
        for (int i = 0; i < half.length; i++) { // 0~256
            fullComplex[i] = half[i];
        }

        for (int i = 1; i < half.length - 1; i++) { // 1~255
            conjugateComplex[i] = half[i].conjugate();
            fullComplex[fullComplex.length - i] = conjugateComplex[i];
        }
        return fullComplex;
    }

    // Reshape [1,1,4,128] -> [1,1,512]
    private float[][][] reshapeBeforeLSTM(float[][][][] input) {
        int inputShape2 = input[0][0].length;
        int inputShape3 = input[0][0][0].length;
        float[][][] matrix = new float[1][1][inputShape2 * inputShape3];
        for (int i = 0; i < inputShape2; i++) {
            for (int j = 0; j < inputShape3; j++) {
                matrix[0][0][j + inputShape3 * i] = input[0][0][i][j];
            }
        }

        return matrix;
    }

    // Reshape [1,1,512] -> [1,1,4,128]
    private float[][][][] reshapeBeforeDecoder(float[][][] input, int shape1, int shape2) {
        float[][][][] matrix = new float[1][1][shape1][shape2];
        for (int i = 0; i < shape1; i++) {
            for (int j = 0; j < shape2; j++) {
                matrix[0][0][i][j] = input[0][0][j + shape2 * i];
            }
        }
        return matrix;
    }

    // Reshape and Padding [1,1,256,1] -> [1,1,257]
    private float[][][] reshapeAndPadding(float[][][][] input) {
        float[][][] matrix = new float[1][1][257];
        matrix[0][0][0] = 0;
        for (int i = 1; i < 257; i++) {
            matrix[0][0][i] = input[0][0][i - 1][0];
        }

        return matrix;
    }

    // Reshape [257] -> [1,1,256,1]
    private float[][][][] reshapeBeforeEncoder(float[] input) {
        float[][][][] matrix = new float[1][1][input.length - 1][1];
        for (int i = 1; i < input.length; i++) {
            matrix[0][0][i - 1][0] = input[i];
        }
        return matrix;
    }


    private float[] reshapeThreeToOne(float[][][] matrix) {
        float[] array = new float[matrix[0][0].length];
        for (int i = 0; i < matrix[0][0].length; i++) {
            array[i] = matrix[0][0][i];
        }

        return array;
    }

    public static double[] floatArrayToDoubleArray(float[] input) {
        if (input == null) {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    private Complex[] createComplex(double[] real, double[] imag) {
        Complex[] complexArray = new Complex[real.length];
        for (int i = 0; i < real.length; i++) {
            complexArray[i] = new Complex(real[i], imag[i]);
        }

        return complexArray;
    }

    private double[] short2double(short[] shortData) {
        int size = shortData.length;
        double[] doubleData = new double[size];
        for (int i = 0; i < size; i++) {
            doubleData[i] = shortData[i] / 32768.0;
        }
        return doubleData;
    }

    private short[] doubleArrayToShortArray(double[] doubleArray) {
        short[] shortArray = new short[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            shortArray[i] = (short) (doubleArray[i] * 32768.0);
        }
        return shortArray;
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

    public static byte[] floatToByte(float d) {
        int i = Float.floatToRawIntBits(d);
        return intToBytes(i);
    }

    public static byte[] intToBytes(int v) {
        byte[] r = new byte[4];
        for (int i = 0; i < 4; i++) {
            r[i] = (byte) ((v >>> (i * 8)) & 0xFF);
        }
        return r;
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

    private double[] shiftLeft(double[] input, int stride) {
        for (int i = 0; i < (frame_len - stride); i++) {
            input[i] = input[stride + i];
        }
        for (int i = (frame_len - stride); i < frame_len; i++) {
            input[i] = 0;
        }
        return input;
    }

    private double[] audioToBuffer(double[] buffer, double[] audio) {
        for (int i = 0; i < audio.length; i++) {
            buffer[buffer.length - audio.length + i] = audio[i];
        }

        return buffer;
    }

//    private float[] getMagnitude(float[] complex, int blockSize) {
//        float[] mag = new float[blockSize / 2];
//        for (int k = 0; k < blockSize / 2; k++) {
//            mag[k] = (float) Math.sqrt(Math.pow(complex[2 * k], 2) + Math.pow(complex[2 * k + 1], 2));
//        }
//        return mag;
//    }
//
//    private float[] getPhase(float[] complex, int blockSize) {
//        float[] phase = new float[blockSize / 2];
//        for (int k = 0; k < blockSize / 2; k++) {
//            phase[k] = (float) Math.atan2(complex[2 * k + 1], complex[2 * k]);
//        }
//        return phase;
//    }

    private double[] getReal(float[] mag, float[] phase, float[] mask) {
        float[] enhancedMag = new float[mag.length];
        double[] real = new double[mag.length];

        for (int k = 0; k < enhancedMag.length; k++) {
            mask[k] = (float) Math.tanh(mask[k]);
            enhancedMag[k] = mag[k] * mask[k];
            real[k] = enhancedMag[k] * (float) Math.cos(phase[k]);
        }

        return real;
    }

    private double[] getImag(float[] mag, float[] phase, float[] mask) {
        float[] enhancedMag = new float[mag.length];
        double[] imag = new double[mag.length];

        for (int k = 0; k < enhancedMag.length; k++) {
            mask[k] = (float) Math.tanh(mask[k]);
            enhancedMag[k] = mag[k] * mask[k];
            imag[k] = enhancedMag[k] * (float) Math.sin(phase[k]);
        }

        return imag;
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

        // 파일 경로(String) 값을 Uri로 변환해서 저장
        //      - Why? : 리사이클러뷰에 들어가는 ArrayList가 Uri를 가지기 때문
        //      - File Path를 알면 File을  인스턴스를 만들어 사용할 수 있기 때문
        audioUri = Uri.parse(audioFileName);
        audioUri2 = Uri.parse(enhancedFileName);

        // 데이터 ArrayList에 담기
        audioList.add(audioUri);
        audioList.add(audioUri2);

        // 데이터 갱신
        audioAdapter.notifyDataSetChanged();

    }

    // 음원 재생 버튼
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record_start:
                if (isRecording) {
                    // 현재 녹음 중 O
                    // 녹음 상태에 따른 변수 아이콘 & 텍스트 변경
                    btn_record_start.setImageDrawable(getResources().getDrawable(R.drawable.btn_record, null)); // 녹음 상태 아이콘 변경
                    text_record.setText("녹음 시작"); // 녹음 상태 텍스트 변경

                    stopRecording();
                    // 녹화 이미지 버튼 변경 및 리코딩 상태 변수값 변경

                } else {
                    // 현재 녹음 중 X
                    /*절차
                     *       1. Audio 권한 체크
                     *       2. 처음으로 녹음 실행한건지 여부 확인
                     * */
                    if (checkAudioPermission()) {
                        // 녹음 상태에 따른 변수 아이콘 & 텍스트 변경
                        isRecording = true; // 녹음 상태 값
                        btn_record_start.setImageDrawable(getResources().getDrawable(R.drawable.btn_record_stop, null)); // 녹음 상태 아이콘 변경
                        text_record.setText("녹음 중"); // 녹음 상태 텍스트 변경

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
            case R.id.btn_se:
//                try {
//                    byte[] output_lite = new byte[0];
//                    if(input_lite != null) {
//                        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(getActivity(), "DTLN_transformed.fflite");
////                        tfLiteModel = loadModelFile(getActivity().getAssets(), "DTLN_transformed.fflite");
//                        Interpreter tflite = new Interpreter(tfliteModel);
//
//                        DtlnTransformed model = DtlnTransformed.newInstance(getContext());
//                        for(int idx=0; idx<(input_lite.length/stride);idx++) {
//                            in_buffer = ShiftToLeft(in_buffer, stride);
//                            in_buffer = AudioToBuffer(in_buffer, input_lite, idx);
//                            ByteBuffer buffer = ByteBuffer.wrap(in_buffer);
//
//                            // Creates inputs for reference.
//                            TensorBuffer inputFeature = TensorBuffer.createFixedSize(new int[]{512}, DataType.FLOAT32);
//                            inputFeature.loadBuffer(buffer);
//                            // Runs model inference and gets result.
////                            DtlnTransformed.Outputs outputs = model.process(inputFeature);
////                            TensorBuffer outputFeature = outputs.getOutputFeature0AsTensorBuffer();
//                            TensorBuffer outputFeature = TensorBuffer.createFixedSize(new int[]{512}, DataType.FLOAT32);
////                            ByteBuffer output = outputFeature.getBuffer();
//
//                            // Running inference
//                            if(null != tflite) {
//                                tflite.run(inputFeature.getBuffer(), outputFeature.getBuffer());
//                            }
//
////                            out = ByteBufferToByteArray(output);
////                            output_lite = concat(output_lite, out);
//                        }
//
//                        String mFilepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Nozae/data";
//                        FileOutputStream fos = null;
//                        try {
//                            fos = new FileOutputStream(mFilepath);
//                        } catch(FileNotFoundException e) {
//                            e.printStackTrace();
//                        }
//
//                        int ret = mAudioRecord.read(output_lite, 0, mBufferSize);  //  AudioRecord의 read 함수를 통해 pcm data 를 읽어옴
//                        Log.d(TAG, "read bytes is " + ret);
//
//                        try {
//                            fos.write(output_lite, 0, mBufferSize);    //  읽어온 readData 를 파일에 write 함
//                        }catch (IOException e){
//                            e.printStackTrace();
//                        }
//
//
//                        // Releases model resources if no longer used.
//                        model.close();
//                    }else{
//                        Toast.makeText(getActivity().getApplicationContext(), "입력 데이터를 선택해주십쇼.", Toast.LENGTH_SHORT).show();
//                    }
//                } catch (IOException e) {
//                    // TODO Handle the exception
//                }

                /* CRN Model */
//                float[] result = new float[48000];
//                if(input_lite != null){
//                    input_lite = Arrays.copyOfRange(input_lite, 0, 48000);
//                    result = noiseReduction.recognizeAudio(input_lite);
//                } else{
//                    Toast.makeText(getActivity().getApplicationContext(), "입력 데이터를 선택해주십쇼.", Toast.LENGTH_SHORT).show();
//                }
//
//                byte[] audio = new byte[result.length * 4];
//                audio = float2Bytes(result);
//                String mFilepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Nozae/data";
//                FileOutputStream fos = null;
//                try {
//                    fos = new FileOutputStream(mFilepath);
//                } catch(FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//
//                int ret = mAudioRecord.read(audio, 0, audio.length);  //  AudioRecord의 read 함수를 통해 pcm data 를 읽어옴
//                Log.d(TAG, "read bytes is " + ret);
//
//                try {
//                    fos.write(audio, 0, audio.length);    //  읽어온 readData 를 파일에 write 함
//                }catch (IOException e){
//                    e.printStackTrace();
//                }


//                float[] floatAudio = new float[audioData.length];
////                float[] imag = new float[in_buffer.length];
////                for (int i = 0; i < in_buffer.length; i++) {
////                    imag[i] = 0;
////                }
//                float[] summary = new float[2 * in_buffer.length];
////                FloatFFT_1D fft = new FloatFFT_1D(in_buffer.length);
//                float[] mag;
//                float[] phase = new float[in_buffer.length / 2];
//                float[] complex = new float[in_buffer.length / 2];
//
//                /* ******************************************************************** */
//                float[] output_lite = new float[0];
//                for (int i = 0; i < audioData.length; i++) {
//                    floatAudio[i] = audioData[i] / 32767.0f;
//                }
//
//                if (audioData != null) {
//                    for (int idx = 0; idx < (floatAudio.length / stride); idx++) {
//                        in_buffer = ShiftToLeft(in_buffer, stride);
//                        in_buffer = AudioToBuffer(in_buffer, floatAudio, idx);
//                        for (int k = 0; k < in_buffer.length; k++) {
////                            summary[2 * k] = in_buffer[k];
//                            summary[2 * k + 1] = imag[k];
//                        }
////                        fft.realForward(summary);
//                        mag = getMagnitude(summary, in_buffer.length);
//                        phase = getPhase(summary, in_buffer.length);
////
////                        float[] out_mask = noiseReduction.runningTFLite_1(in_buffer);
////
////                        complex = getComplex(mag, phase, out_mask); // complex를 real / imag로 나누어
////                        fft.realInverse(complex, true);
//
////                        result = Arrays.copyOfRange(result, 0, stride);
////                        output_lite = concat_f(output_lite, result);
//                    }
//                } else {
//                    Toast.makeText(getActivity().getApplicationContext(), "입력 데이터를 선택해주십쇼.", Toast.LENGTH_SHORT).show();
//                }
//                byte[] audio = new byte[output_lite.length * 4];
//                audio = float2Bytes(output_lite);
//                String mFilepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Nozae/data";
//                FileOutputStream fos = null;
//                try {
//                    fos = new FileOutputStream(mFilepath);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//
//                int ret2 = mAudioRecord.read(audio, 0, audio.length);  //  AudioRecord의 read 함수를 통해 pcm data 를 읽어옴
////                Log.d(TAG, "read bytes is " + ret);
//
//                try {
//                    fos.write(audio, 0, audio.length);    //  읽어온 readData 를 파일에 write 함
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                break;
            case R.id.btn_play:
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.noisy);
                    mediaPlayer.start();
                } else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(pauseposition);
                    mediaPlayer.start();
                }
                break;

            case R.id.btn_pause:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                    pauseposition = mediaPlayer.getCurrentPosition();
                    Log.d("pause check", ":" + pauseposition);
                }
                break;
            case R.id.btn_stop:
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer = null;
                }
                break;
        }
    }
}

