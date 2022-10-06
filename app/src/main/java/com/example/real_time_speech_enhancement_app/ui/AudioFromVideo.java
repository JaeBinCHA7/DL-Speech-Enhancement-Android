package com.example.real_time_speech_enhancement_app.ui;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.example.real_time_speech_enhancement_app.BuildConfig.DEBUG;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioFromVideo {
    private String audio, videopath;
    private MediaCodec audioDeCoder;
    private MediaCodec audioEncoder;
    private MediaExtractor audioExtractor;
    private MediaExtractor videoExtractor;
    private MediaFormat audioformat;
    private MediaFormat videoformat;
    private String audiomime, videomime;
    private MediaMuxer muxer;
    private static final int MAX_BUFFER_SIZE = 256 * 1024;


    boolean videoExtractorDone = false;
    boolean audioExtractorDone = false;
    boolean audioDecoderDone = false;
    boolean audioEncoderDone = false;
    boolean muxing = false;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private static final int TIMEOUT_USEC = 10000;

    //times
    long lastPresentationTimeVideoExtractor = 0;
    long lastPresentationTimeAudioExtractor = 0;
    long lastPresentationTimeAudioDecoder = 0;
    long lastPresentationTimeAudioEncoder = 0;

    // We will get these from the decoders when notified of a format change.
    MediaFormat decoderOutputAudioFormat = null;
    // We will get these from the encoders when notified of a format change.
    MediaFormat encoderOutputAudioFormat = null;
    // We will determine these once we have the output format.
    int outputAudioTrack = -1;
    // Whether things are done on the video side.

    int pendingAudioDecoderOutputBufferIndex = -1;
    int videoExtractedFrameCount = 0;
    int audioExtractedFrameCount = 0;
    int audioDecodedFrameCount = 0;
    int audioEncodedFrameCount = 0;

    byte[] byteArray = new byte[0];

    public AudioFromVideo(String srcVideo, String destAudio) {
        this.audio = destAudio;
        this.videopath = srcVideo;
        audioExtractor = new MediaExtractor();
        videoExtractor = new MediaExtractor();
        audioDeCoder = null;
        audioEncoder = null;
        muxer = null;

        init();
    }


    public void start() {
        new AudioService(audioDeCoder, audioExtractor, audio).start();
    }

    private class AudioService extends Thread {
        private MediaCodec mMediaCodec;
        private MediaExtractor mMediaExtractor;
        private String destFile;

        AudioService(MediaCodec amc, MediaExtractor ame, String destFile) {
            mMediaCodec = amc;
            mMediaExtractor = ame;
            this.destFile = destFile;
        }

        public void run() {
            try {
                //OutputStream os = new FileOutputStream(new File(destFile));
                MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
                ByteBuffer videoInputBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);

                byte[] datamerge = new byte[0];

                ByteBuffer[] audioDecoderInputBuffers = null;
                ByteBuffer[] audioDecoderOutputBuffers = null;
                ByteBuffer[] audioEncoderInputBuffers = null;
                ByteBuffer[] audioEncoderOutputBuffers = null;
                MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;
                MediaCodec.BufferInfo audioEncoderOutputBufferInfo = null;

                audioDecoderInputBuffers = audioDeCoder.getInputBuffers();
                audioDecoderOutputBuffers = audioDeCoder.getOutputBuffers();
                audioEncoderInputBuffers = audioEncoder.getInputBuffers();
                audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
                audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
                audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

                //ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                //OutputStream os = new FileOutputStream(new File(destFile));
                //ArrayList<Short> sample = new ArrayList<>();
                long count = 0;

                MediaMetadataRetriever retrieverTest = new MediaMetadataRetriever();
                retrieverTest.setDataSource(videopath);
                String degreesStr = retrieverTest.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (degreesStr != null) {
                    Integer degrees = Integer.parseInt(degreesStr);
                    if (degrees >= 0) {
                        muxer.setOrientationHint(degrees);
                    }
                }


                while (!videoExtractorDone || !audioEncoderDone) {
                    if (true) {
                        Log.d(TAG, String.format("ex:%d at %d | de:%d at %d | en:%d at %d ",
                                audioExtractedFrameCount, lastPresentationTimeAudioExtractor,
                                audioDecodedFrameCount, lastPresentationTimeAudioDecoder,
                                audioEncodedFrameCount, lastPresentationTimeAudioEncoder
                        ));
                    }

                    /**
                     * 비디오 추출 + 먹싱
                     */
                    while (!videoExtractorDone && muxing) {

                        try {
                            videoBufferInfo.size = videoExtractor.readSampleData(videoInputBuffer, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (videoBufferInfo.size < 0) {
                            videoBufferInfo.size = 0;
                            videoExtractorDone = true;
                        } else {
                            videoBufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                            videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                            lastPresentationTimeVideoExtractor = videoBufferInfo.presentationTimeUs;
                            int videoflag = videoExtractor.getSampleFlags();
                            Log.d("add video track", "");
                            muxer.writeSampleData(videoTrackIndex, videoInputBuffer, videoBufferInfo);
                            videoExtractor.advance();
                            videoExtractedFrameCount++;
                        }
                    }


                    /** 오디오를 추출하고 디코딩. **/

                    while (!audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                        int decoderInputBufferIndex = audioDeCoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: returned input buffer: " + decoderInputBufferIndex);
                        }
                        ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                        int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                        long presentationTime = audioExtractor.getSampleTime();
                        lastPresentationTimeAudioExtractor = presentationTime;
                        if (DEBUG) {
                            Log.d(TAG, "audio extractor: returned buffer of size " + size);
                            Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);
                        }
                        if (size >= 0) {
                            audioDeCoder.queueInputBuffer(
                                    decoderInputBufferIndex,
                                    0,
                                    size,
                                    presentationTime,
                                    audioExtractor.getSampleFlags());
                            /*ByteBuffer bb = mMediaCodec.getOutputBuffer(decoderInputBufferIndex);
                            byte[] data = new byte[videoBufferInfo.size];
                            bb.get(data);*/
                        }
                        audioExtractorDone = !audioExtractor.advance();
                        if (audioExtractorDone) {
                            decoderInputBufferIndex = audioDeCoder.dequeueInputBuffer(-1);
                            if (DEBUG) Log.d(TAG, "audio extractor: EOS" + decoderInputBufferIndex);
                            audioDeCoder.queueInputBuffer(
                                    decoderInputBufferIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                        audioExtractedFrameCount++;
                        // We extracted a frame, let's try something else next.
                        break;
                    }

                    /**
                     * 오디오 디코더에서 출력 프레임을 폴링합니다.
                     * 인코더에 공급할 보류 중인 버퍼가 이미 있는 경우 폴링 X.
                     */
                    while (!audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1 && (encoderOutputAudioFormat == null || muxing)) {
                        int decoderOutputBufferIndex =
                                audioDeCoder.dequeueOutputBuffer(
                                        audioDecoderOutputBufferInfo, TIMEOUT_USEC);
                        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            if (DEBUG) Log.d(TAG, "no audio decoder output buffer");
                            break;
                        }
                        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            if (DEBUG) Log.d(TAG, "audio decoder: output buffers changed");
                            audioDecoderOutputBuffers = audioDeCoder.getOutputBuffers();
                            break;
                        }
                        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            decoderOutputAudioFormat = audioDeCoder.getOutputFormat();
                            if (DEBUG) {
                                Log.d(TAG, "audio decoder: output format changed: "
                                        + decoderOutputAudioFormat);
                            }
                            break;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: returned output buffer: "
                                    + decoderOutputBufferIndex);
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: returned buffer of size "
                                    + audioDecoderOutputBufferInfo.size);
                        }
                        ByteBuffer decoderOutputBuffer =
                                audioDecoderOutputBuffers[decoderOutputBufferIndex];
                        if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                                != 0) {
                            if (DEBUG) Log.d(TAG, "audio decoder: codec config buffer");
                            audioDeCoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                            break;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: returned buffer for time "
                                    + audioDecoderOutputBufferInfo.presentationTimeUs);
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: output buffer is now pending: "
                                    + pendingAudioDecoderOutputBufferIndex);
                        }
                        ByteBuffer bb = mMediaCodec.getOutputBuffer(decoderOutputBufferIndex);
                        byte[] data = new byte[audioDecoderOutputBufferInfo.size];
                        bb.get(data);
                        datamerge = concatByteArray(datamerge, data);

//                        byte[] byteArray = concatByteArray(byteArray, data);
                        //os.write(data);


                        pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
                        audioDecodedFrameCount++;
                        // We extracted a pending frame, let's try something else next.
                        break;
                    }


                    /** 디코딩된 오디오 버퍼를 오디오 인코더에 공급.*/
                    while (pendingAudioDecoderOutputBufferIndex != -1) {
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: attempting to process pending buffer: "
                                    + pendingAudioDecoderOutputBufferIndex);
                        }
                        int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (encoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            if (DEBUG) Log.d(TAG, "no audio encoder input buffer");
                            break;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio encoder: returned input buffer: " + encoderInputBufferIndex);
                        }
                        ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
                        int size = audioDecoderOutputBufferInfo.size;
                        long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;
                        lastPresentationTimeAudioDecoder = presentationTime;
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: processing pending buffer: "
                                    + pendingAudioDecoderOutputBufferIndex);
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio decoder: pending buffer of size " + size);
                            Log.d(TAG, "audio decoder: pending buffer for time " + presentationTime);
                        }
                        if (size >= 0) {
                            ByteBuffer decoderOutputBuffer =
                                    audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex]
                                            .duplicate();

                            // Encoding
                            decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
                            decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
                            encoderInputBuffer.position(0);
                            encoderInputBuffer.put(decoderOutputBuffer);
                            audioEncoder.queueInputBuffer(
                                    encoderInputBufferIndex,
                                    0,
                                    size,
                                    presentationTime,
                                    audioDecoderOutputBufferInfo.flags);
                        }
                        audioDeCoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
                        pendingAudioDecoderOutputBufferIndex = -1;
                        if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (DEBUG) Log.d(TAG, "audio decoder: EOS");
                            audioDecoderDone = true;
                        }
                        // We enqueued a pending frame, let's try something else next.
                        break;
                    }

                    /** 오디오 인코더에서 프레임을 폴링하여 muxer로 보낸다.*/
                    while (!audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
                        int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
                                audioEncoderOutputBufferInfo, TIMEOUT_USEC);
                        if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            if (DEBUG) Log.d(TAG, "no audio encoder output buffer");
                            break;
                        }
                        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            if (DEBUG) Log.d(TAG, "audio encoder: output buffers changed");
                            audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
                            break;
                        }
                        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            encoderOutputAudioFormat = audioEncoder.getOutputFormat();
                            if (DEBUG) {
                                Log.d(TAG, "audio encoder: output format changed");
                            }
                            if (outputAudioTrack >= 0) {
                                Log.e(TAG, "audio encoder changed its output format again?");
                            }
                            break;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio encoder: returned output buffer: "
                                    + encoderOutputBufferIndex);
                            Log.d(TAG, "audio encoder: returned buffer of size "
                                    + audioEncoderOutputBufferInfo.size);
                        }
                        ByteBuffer encoderOutputBuffer =
                                audioEncoderOutputBuffers[encoderOutputBufferIndex];
                        if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                                != 0) {
                            if (DEBUG) Log.d(TAG, "audio encoder: codec config buffer");
                            // Simply ignore codec config buffers.
                            audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                            break;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "audio encoder: returned buffer for time "
                                    + audioEncoderOutputBufferInfo.presentationTimeUs);
                        }
                        if (audioEncoderOutputBufferInfo.size != 0) {
                            lastPresentationTimeAudioEncoder = audioEncoderOutputBufferInfo.presentationTimeUs;
                            muxer.writeSampleData(audioTrackIndex, encoderOutputBuffer, audioEncoderOutputBufferInfo);
                        }
                        if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                != 0) {
                            if (DEBUG) Log.d(TAG, "audio encoder: EOS");
                            audioEncoderDone = true;
                        }
                        audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                        audioEncodedFrameCount++;
                        // We enqueued an encoded frame, let's try something else next.
                        break;
                    }

                    if (!muxing && (encoderOutputAudioFormat != null)) {

                        videoTrackIndex = muxer.addTrack(videoformat);
                        Log.d("", "muxer: adding video track." + videoTrackIndex);

                        audioTrackIndex = muxer.addTrack(encoderOutputAudioFormat);
                        Log.d("", "muxer: adding audio track." + audioTrackIndex);

                        muxer.start();
                        Log.d("", "muxer: starting");
                        muxing = true;
                    }

                    /**
                     * 오디오 비디오 처리 끝
                     */

                    Log.d("", "encoded and decoded audio frame counts should match. decoded:" + audioDecodedFrameCount + " encoded:" + audioEncodedFrameCount);
                    Log.d("", "decoded frame count should be less than extracted frame coun. decoded:" + audioDecodedFrameCount + " extracted:" + audioExtractedFrameCount);
                    Log.d("", "no audio frame should be pending " + pendingAudioDecoderOutputBufferIndex);

                }
                audioEncoder.stop();
                audioEncoder.release();

                audioDeCoder.stop();
                audioDeCoder.release();


                muxer.stop();
                muxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private byte[] concatByteArray(byte[] array1, byte[] array2) {
        int lenArray1 = array1.length;
        int lenArray2 = array2.length;

        byte[] concatArray = new byte[lenArray1 + lenArray2];

        System.arraycopy(array1, 0, concatArray, 0, lenArray1);
        System.arraycopy(array2, 0, concatArray, lenArray1, lenArray2);

        return concatArray;
    }

    public void init() {
        try {
            audioExtractor.setDataSource(videopath);
            videoExtractor.setDataSource(videopath);

            audioformat = audioExtractor.getTrackFormat(1);
            videoformat = videoExtractor.getTrackFormat(0);
            Log.d("audioformat: ", "" + videoformat);

            //videoformat = MediaFormat.createVideoFormat(videomime, 1280, 720);
            //videoformat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            //videoformat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            //videoformat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);


            audioExtractor.selectTrack(1);
            videoExtractor.selectTrack(0);

            /*int numTracks = audioExtrator.getTrackCount();
            Log.d("", "numTracks: " + numTracks);*/

            audiomime = audioformat.getString(MediaFormat.KEY_MIME); //
            videomime = videoformat.getString(MediaFormat.KEY_MIME); //
            Log.d("audiomime :", audiomime);
            Log.d("videomime :", videomime);

            audioDeCoder = MediaCodec.createDecoderByType(audiomime);
            audioDeCoder.configure(audioformat, null, null, 0);
            audioDeCoder.start();

            audioEncoder = MediaCodec.createEncoderByType(audiomime);
            audioEncoder.configure(audioformat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            audioEncoder.start();


            muxer = new MediaMuxer("/storage/emulated/0/Movies/temp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //MediaMetadataRetriever retrieverTest = new MediaMetadataRetriever();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
