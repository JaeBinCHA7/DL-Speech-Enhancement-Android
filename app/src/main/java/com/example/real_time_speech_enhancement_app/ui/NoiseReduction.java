package com.example.real_time_speech_enhancement_app.ui;

import android.app.Activity;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class NoiseReduction {

    private Interpreter.Options tfOptions = new Interpreter.Options();
    private final Interpreter.Options tfOptions1 = new Interpreter.Options();
    private final Interpreter.Options tfOptions2 = new Interpreter.Options();
    private final Interpreter.Options tfOptions3 = new Interpreter.Options();
    private final Interpreter.Options tfOptions4 = new Interpreter.Options();
    private final Interpreter.Options tfOptions5 = new Interpreter.Options();
    private final Interpreter.Options tfOptions6 = new Interpreter.Options();
    private final Interpreter.Options tfOptions7 = new Interpreter.Options();
    private final Interpreter.Options tfOptions8 = new Interpreter.Options();
    private final Interpreter.Options tfOptions9 = new Interpreter.Options();
    private final Interpreter.Options tfOptions10 = new Interpreter.Options();
    private final Interpreter.Options tfOptions11 = new Interpreter.Options();
    private final Interpreter.Options tfOptions12 = new Interpreter.Options();
    private final Interpreter.Options tfOptions13 = new Interpreter.Options();

    private MappedByteBuffer tfModel_1;
    private MappedByteBuffer tfModel_2;
    private MappedByteBuffer tfModel_3;
    private MappedByteBuffer tfModel_4;
    private MappedByteBuffer tfModel_5;
    private MappedByteBuffer tfModel_6;
    private MappedByteBuffer tfModel_7;
    private MappedByteBuffer tfModel_8;
    private MappedByteBuffer tfModel_9;
    private MappedByteBuffer tfModel_10;
    private MappedByteBuffer tfModel_11;
    private MappedByteBuffer tfModel_12;
    private MappedByteBuffer tfModel_13;

    private Interpreter tflite_1;
    private Interpreter tflite_2;
    private Interpreter tflite_3;
    private Interpreter tflite_4;
    private Interpreter tflite_5;
    private Interpreter tflite_6;
    private Interpreter tflite_7;
    private Interpreter tflite_8;
    private Interpreter tflite_9;
    private Interpreter tflite_10;
    private Interpreter tflite_11;
    private Interpreter tflite_12;
    private Interpreter tflite_13;

    NnApiDelegate nnApiDelegate = null;
    NnApiDelegate nnApiDelegate1 = null;
    NnApiDelegate nnApiDelegate2 = null;
    NnApiDelegate nnApiDelegate3 = null;
    NnApiDelegate nnApiDelegate4 = null;
    NnApiDelegate nnApiDelegate5 = null;
    NnApiDelegate nnApiDelegate6 = null;
    NnApiDelegate nnApiDelegate7 = null;
    NnApiDelegate nnApiDelegate8 = null;
    NnApiDelegate nnApiDelegate9 = null;
    NnApiDelegate nnApiDelegate10 = null;
    NnApiDelegate nnApiDelegate11 = null;
    NnApiDelegate nnApiDelegate12 = null;
    NnApiDelegate nnApiDelegate13 = null;



    public NoiseReduction(Activity activity, String modelPath_1, String modelPath_2, String modelPath_3,
                          String modelPath_4, String modelPath_5, String modelPath_6, String modelPath_7,
                          String modelPath_8, String modelPath_9, String modelPath_10, String modelPath_11,
                          String modelPath_12, String modelPath_13) throws IOException {

        /**
         load TF Lite model_1
         */
//        NnApiDelegate nnApiDelegate1 = null;
//        NnApiDelegate nnApiDelegate2 = null;
//        NnApiDelegate nnApiDelegate3 = null;
//        NnApiDelegate nnApiDelegate4 = null;
//        NnApiDelegate nnApiDelegate5 = null;
//        NnApiDelegate nnApiDelegate6 = null;
//        NnApiDelegate nnApiDelegate7 = null;
//        NnApiDelegate nnApiDelegate8 = null;
//        NnApiDelegate nnApiDelegate9 = null;
//        NnApiDelegate nnApiDelegate10 = null;
//        NnApiDelegate nnApiDelegate11 = null;
//        NnApiDelegate nnApiDelegate12 = null;
//        NnApiDelegate nnApiDelegate13 = null;
//
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate1 = new NnApiDelegate();
//            nnApiDelegate2 = new NnApiDelegate();
//            nnApiDelegate3 = new NnApiDelegate();
//            nnApiDelegate4   = new NnApiDelegate();
//            nnApiDelegate5 = new NnApiDelegate();
//            nnApiDelegate6 = new NnApiDelegate();
//            nnApiDelegate7 = new NnApiDelegate();
//            nnApiDelegate8 = new NnApiDelegate();
//            nnApiDelegate9 = new NnApiDelegate();
//            nnApiDelegate10 = new NnApiDelegate();
//            nnApiDelegate11 = new NnApiDelegate();
//            nnApiDelegate12 = new NnApiDelegate();
//            nnApiDelegate13 = new NnApiDelegate();
//
//            tfOptions1.addDelegate(nnApiDelegate1);
//            tfOptions2.addDelegate(nnApiDelegate2);
//            tfOptions3.addDelegate(nnApiDelegate3);
//            tfOptions4.addDelegate(nnApiDelegate4);
//            tfOptions5.addDelegate(nnApiDelegate5);
//            tfOptions6.addDelegate(nnApiDelegate6);
//            tfOptions7.addDelegate(nnApiDelegate7);
//            tfOptions8.addDelegate(nnApiDelegate8);
//            tfOptions9.addDelegate(nnApiDelegate9);
//            tfOptions10.addDelegate(nnApiDelegate10);
//            tfOptions11.addDelegate(nnApiDelegate11);
//            tfOptions12.addDelegate(nnApiDelegate12);
//            tfOptions13.addDelegate(nnApiDelegate13);
//        }

        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate = new NnApiDelegate();
//            tfOptions.addDelegate(nnApiDelegate);
//        }

//        tfOptions.setUseNNAPI(true);
        tfOptions.setNumThreads(-1);

        tfModel_1 = FileUtil.loadMappedFile(activity, modelPath_1);
        tflite_1 = new Interpreter(tfModel_1, tfOptions);

        /**
         load TF Lite model_2
         */
//        NnApiDelegate nnApiDelegate2 = null;
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate2 = new NnApiDelegate();
//            tfOptions2.addDelegate(nnApiDelegate2);
//        }
        tfModel_2 = FileUtil.loadMappedFile(activity, modelPath_2);
        tflite_2 = new Interpreter(tfModel_2, tfOptions);

        /**
         load TF Lite model_3
         */
//        NnApiDelegate nnApiDelegate3 = null;
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate3 = new NnApiDelegate();
//            tfOptions3.addDelegate(nnApiDelegate3);
//        }

        tfModel_3 = FileUtil.loadMappedFile(activity, modelPath_3);
        tflite_3 = new Interpreter(tfModel_3, tfOptions);

        /**
         load TF Lite model_4
         */
//        NnApiDelegate nnApiDelegate4 = null;
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate4 = new NnApiDelegate();
//            tfOptions4.addDelegate(nnApiDelegate4);
//        }

        tfModel_4 = FileUtil.loadMappedFile(activity, modelPath_4);
        tflite_4 = new Interpreter(tfModel_4, tfOptions);


        /**
         load TF Lite model_5
         */
//        NnApiDelegate nnApiDelegate5 = null;
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate5 = new NnApiDelegate();
//            tfOptions5.addDelegate(nnApiDelegate5);
//        }

        tfModel_5 = FileUtil.loadMappedFile(activity, modelPath_5);
        tflite_5 = new Interpreter(tfModel_5, tfOptions);


        /**
         load TF Lite model_6
         */
//        NnApiDelegate nnApiDelegate6 = null;
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate6 = new NnApiDelegate();
//            tfOptions6.addDelegate(nnApiDelegate6);
//        }

        tfModel_6 = FileUtil.loadMappedFile(activity, modelPath_6);
        tflite_6 = new Interpreter(tfModel_6, tfOptions);


        /**
         load TF Lite model_7
         */
//        NnApiDelegate nnApiDelegate7 = null;
//        // Initialize interpreter with NNAPI delegate for Android Pie or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            nnApiDelegate7 = new NnApiDelegate();
//            tfOptions7.addDelegate(nnApiDelegate7);
//        }

        tfModel_7 = FileUtil.loadMappedFile(activity, modelPath_7);
        tflite_7 = new Interpreter(tfModel_7, tfOptions);

        /**
         load TF Lite model_8
         */
        tfModel_8 = FileUtil.loadMappedFile(activity, modelPath_8);
        tflite_8 = new Interpreter(tfModel_8, tfOptions);

        /**
         load TF Lite model_9
         */
        tfModel_9 = FileUtil.loadMappedFile(activity, modelPath_9);
        tflite_9 = new Interpreter(tfModel_9, tfOptions);

        /**
         load TF Lite model_10
         */
        tfModel_10 = FileUtil.loadMappedFile(activity, modelPath_10);
        tflite_10 = new Interpreter(tfModel_10, tfOptions);

        /**
         load TF Lite model_11
         */
        tfModel_11 = FileUtil.loadMappedFile(activity, modelPath_11);
        tflite_11 = new Interpreter(tfModel_11, tfOptions);

        /**
         load TF Lite model_12
         */
        tfModel_12 = FileUtil.loadMappedFile(activity, modelPath_12);
        tflite_12 = new Interpreter(tfModel_12, tfOptions);

        /**
         load TF Lite model_13
         */
        tfModel_13 = FileUtil.loadMappedFile(activity, modelPath_13);
        tflite_13 = new Interpreter(tfModel_13, tfOptions);

//        // reads type and shape of input tensors of tf model
//        int[] inputShape_13 = tflite_13.getInputTensor(0).shape(); // [1, 1, 257]
//        DataType inputDataType_13 = tflite_13.getInputTensor(0).dataType(); // FLOAT32
//        // create input tensor
//        inputBuffer_13 = TensorBuffer.createFixedSize(inputShape_13, inputDataType_13);
//        // reads type and shape of output tensors of tf model
//        int[] outputShape_13 = tflite_13.getOutputTensor(0).shape(); // [1, 1, 257]
//        DataType outputDataType_13 = tflite_13.getOutputTensor(0).dataType();
//        //create output tensor
//        outputBuffer_13 = TensorBuffer.createFixedSize(outputShape_13, outputDataType_13);
    }

    // CRN - Conv1
    public float[][][][] runningTFLite_1(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][128][16]);
        tflite_1.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv2
    public float[][][][] runningTFLite_2(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][64][32]);
        tflite_2.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv3
    public float[][][][] runningTFLite_3(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][32][64]);
        tflite_3.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv4
    public float[][][][] runningTFLite_4(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][16][128]);
        tflite_4.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);
        return outData;
    }

    // CRN - Conv 5
    public float[][][][] runningTFLite_5(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][8][128]);
        tflite_5.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv 6
    public float[][][][] runningTFLite_6(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][4][128]);
        tflite_6.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - LSTM
    public float[][][][] runningTFLite_7(float[][][] inputData, float[][][] state) {
        Object[] inputArray = {inputData, state};
//        float[][][][][] inputArray = new float[][][][][]{{inputData}, {state}};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][128][2]);
        outputMap.put(1, new float[1][1][512]);
        tflite_7.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][] outState = (float[][][]) outputMap.get(0);
        float[][][] outData = (float[][][]) outputMap.get(1);


        return new float[][][][] {outData, outState};
    }

    // CRN - Conv T1
    public float[][][][] runningTFLite_8(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][8][128]);
        tflite_8.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv T2
    public float[][][][] runningTFLite_9(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][16][128]);
        tflite_9.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv T3
    public float[][][][] runningTFLite_10(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][32][64]);
        tflite_10.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv T4
    public float[][][][] runningTFLite_11(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][64][32]);
        tflite_11.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv T5
    public float[][][][] runningTFLite_12(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][128][16]);
        tflite_12.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    // CRN - Conv T6
    public float[][][][] runningTFLite_13(float[][][][] inputData) {
        Object[] inputArray = {inputData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][1][256][1]);
        tflite_13.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }

    public void closeTFLite(){
        // Unload delegate
        tflite_1.close();
        tflite_2.close();
        tflite_3.close();
        tflite_4.close();
        tflite_5.close();
        tflite_6.close();
        tflite_7.close();
        tflite_8.close();
        tflite_9.close();
        tflite_10.close();
        tflite_11.close();
        tflite_12.close();
        tflite_13.close();

        if(null != nnApiDelegate) {
            nnApiDelegate.close();
        }
    }
}