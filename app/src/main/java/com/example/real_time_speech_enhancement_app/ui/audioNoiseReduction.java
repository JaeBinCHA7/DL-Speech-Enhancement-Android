package com.example.real_time_speech_enhancement_app.ui;

import android.app.Activity;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class audioNoiseReduction {
    private Interpreter.Options tfOptions = new Interpreter.Options();
    private MappedByteBuffer tfModel_1;
    private Interpreter tflite_1;
    NnApiDelegate nnApiDelegate = null;

    public audioNoiseReduction(Activity activity, String modelPath_1) throws IOException {

        /**
         load TF Lite model_1
         */
        tfOptions.setNumThreads(-1);

        tfModel_1 = FileUtil.loadMappedFile(activity, modelPath_1);
        tflite_1 = new Interpreter(tfModel_1, tfOptions);

    }

    // CRN - Conv1
    public float[][][][] runningTFLite_1(float[][][][] inputData) {

        tflite_1.resizeInput(0, new int[]{1, inputData[0].length, 256, 1}, true);

        Object[] inputArray = {inputData};

        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, new float[1][inputData[0].length][256][1]);
        tflite_1.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] outData = (float[][][][]) outputMap.get(0);

        return outData;
    }


    public void closeTFLite() {
        // Unload delegate
        tflite_1.close();

        if (null != nnApiDelegate) {
            nnApiDelegate.close();
        }
    }
}
