//package com.example.real_time_speech_enhancement_app.ui;
//
//import android.content.Context;
//import android.content.res.AssetFileDescriptor;
//import android.content.res.AssetManager;
//
//import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.Tensor;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.channels.FileChannel;
//
//public class SpeechEnhancement {
//    private static final String MODEL_NAME = "pruned_model_s80_DEFAULT.tflite"; // 파일명은 변하지 않는 값이므로 final 변수로 선언
//    Context context;
//    Interpreter interpreter = null;
//
//    int modelInputWidth, modelInputHeight, modelInputChannel;
//
//    // 모델을 불러올 때 assets 폴더를 참조함. 이때 앱 컨텍스트가 필요. 컨텍스트를 입력받아 멤버 변수로 가지고 있게 함.
//    public SpeechEnhancement(Context context) {
//        this.context = context;
//    }
//
//    // assets 폴더에서 tflite 파일을 읽어노는 함수. tflite 파일명을 입력받아 ByteBuffer 클래스로 모델을 반환.
//    private ByteBuffer loadModelFile(String modelName) throws IOException {
//        AssetManager am = context.getAssets(); // AssetManaget는 assets 폴더에 저장된 리소스에 접근하기 위한 기능을 제공.
//        AssetFileDescriptor afd = am.openFd(modelName);
//        FileInputStream fis = new FileInputStream(afd.getFileDescriptor()); // FileDescriptor를 얻고 파일의 읽기/쓰기가 가능
//        FileChannel fc = fis.getChannel();
//        long startOffset = afd.getStartOffset();
//        long declaredLength = afd.getDeclaredLength();
//
//        // map() 함수에 길이와 오프셋을 전달하면 ByteBuffer 클래스를 상속한 MappedByteBuffer 객체를 반환.
//        return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength); // tflite 파일을 ByteBuffer 형으로 읽어오게 됨.
//    }
//
//    public void init() throws IOException{
//        ByteBuffer model = loadModelFile(MODEL_NAME);
//        model.order(ByteOrder.nativeOrder()); // 시스템의 byteOrder 값과 동일하게 설ㅈ
//        interpreter = new Interpreter(model);
//
//        initModelShape();
//    }
//
//    private  void initModelShape(){
//        Tensor inputTensor = interpreter.getInputTensor(0);
//        int[] inputShape = inputTensor.shape();
//        modelInputChannel = inputShape[0];
//        modelInputWidth = inputShape[1];
//        modelInputHeight = inputShape[2];
//
//    }
//}
