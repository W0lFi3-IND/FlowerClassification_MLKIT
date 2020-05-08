package com.example.catsvsdogs;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
CameraView mCameraView;
    FirebaseModelInterpreter interpreter;
    FirebaseModelInterpreterOptions options;
    FirebaseModelInputOutputOptions inputOutputOptions;
    FirebaseCustomLocalModel localModel;
    String ans="";
    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.stop();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.cameraview);

         localModel = new FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("model.tflite")
                .build();

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.start();
                mCameraView.captureImage();
            }
        });
        mCameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,224,224,true);
                mCameraView.stop();
                try {
                    classifier(bitmap);
                } catch (FirebaseMLException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });
    }

    void classifier(Bitmap bitmap) throws FirebaseMLException {

        try {
             options =
                    new FirebaseModelInterpreterOptions.Builder(localModel).build();
            interpreter = FirebaseModelInterpreter.getInstance(options);
        } catch (FirebaseMLException e) {
            // ...
        }
         inputOutputOptions =
                new FirebaseModelInputOutputOptions.Builder()
                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 5})
                        .build();
        int batchNum = 0;
        float[][][][] input = new float[1][224][224][3];
        for (int x = 0; x < 224; x++) {
            for (int y = 0; y < 224; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                // model. For example, some models might require values to be normalized
                // to the range [0.0, 1.0] instead.
                input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
                input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
                input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;
            }
        }
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)  // add() as many input arrays as your model requires
                .build();
        interpreter.run(inputs, inputOutputOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) {
                                float[][] output = result.getOutput(0);
                                float[] probabilities = output[0];
                                BufferedReader reader = null;
                                try {
                                    reader = new BufferedReader(
                                            new InputStreamReader(getAssets().open("dict.txt")));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                for (int i = 0; i < probabilities.length; i++) {
                                    String label = null;
                                    try {
                                        label = reader.readLine();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Log.i("MLKit", String.format("%s: %1.4f", label, probabilities[i]));

                                   ans +="\n"+String.format("%s: %1.4f", label, probabilities[i]);

                                }
                                TextView tv = findViewById(R.id.textView);
                                tv.setText("");
                                tv.setText(ans);
                                ans="";
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });


    }


}