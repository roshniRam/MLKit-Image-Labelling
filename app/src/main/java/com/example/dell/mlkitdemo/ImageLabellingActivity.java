package com.example.dell.mlkitdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource;
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource;
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ImageLabellingActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageView mImageView;
    Button selectImageButton;
    Button detectImageButton;
    Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_labelling);

        mImageView = findViewById(R.id.display_image);
        selectImageButton = findViewById(R.id.select_image_button);
        detectImageButton = findViewById(R.id.detect_image);
        imageBitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.rose);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(view);
            }
        });

        mImageView.setImageBitmap(imageBitmap);

//        FirebaseVisionLabelDetectorOptions options =
//                new FirebaseVisionLabelDetectorOptions.Builder()
//                        .setConfidenceThreshold(0.8f)
//                        .build();
//
//        detectImageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.test6);
//                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
//
//                FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
//                        .getVisionLabelDetector();
//
//                Task<List<FirebaseVisionLabel>> result =
//                        detector.detectInImage(image)
//                                .addOnSuccessListener(
//                                        new OnSuccessListener<List<FirebaseVisionLabel>>() {
//                                            @Override
//                                            public void onSuccess(List<FirebaseVisionLabel> labels) {
//                                                // Task completed successfully
//                                                // ...
//                                                for (FirebaseVisionLabel label: labels) {
//                                                    String text = label.getLabel();
//                                                    String entityId = label.getEntityId();
//                                                    float confidence = label.getConfidence();
//
//                                                    Toast.makeText(ImageLabellingActivity.this, text, Toast.LENGTH_LONG).show();
//                                                }
//                                            }
//                                        })
//                                .addOnFailureListener(
//                                        new OnFailureListener() {
//                                            @Override
//                                            public void onFailure(@NonNull Exception e) {
//                                                // Task failed with an exception
//                                                // ...
//                                            }
//                                        });
//
//            }
//        });

        FirebaseModelDownloadConditions.Builder conditionsBuilder =
                new FirebaseModelDownloadConditions.Builder().requireWifi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Enable advanced conditions on Android Nougat and newer.
            conditionsBuilder = conditionsBuilder
                    .requireCharging()
                    .requireDeviceIdle();
        }
        FirebaseModelDownloadConditions conditions = conditionsBuilder.build();

// Build a FirebaseCloudModelSource object by specifying the name you assigned the model
// when you uploaded it in the Firebase console.
        FirebaseCloudModelSource cloudSource = new FirebaseCloudModelSource.Builder("graph")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager.getInstance().registerCloudModelSource(cloudSource);

        FirebaseLocalModelSource localSource =
                new FirebaseLocalModelSource.Builder("my_local_model")  // Assign a name for this model
                        .setAssetFilePath("optimized_graph.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModelSource(localSource);



        detectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsynCaller().execute();
            }
        });




    }

    public void selectImage(View v){
        PopupMenu popupMenu = new PopupMenu(this,v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.options_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.take_photo:
                if (ContextCompat.checkSelfPermission(ImageLabellingActivity.this, android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    if (ActivityCompat.shouldShowRequestPermissionRationale(ImageLabellingActivity.this,
                            android.Manifest.permission.CAMERA)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(ImageLabellingActivity.this,
                                new String[]{Manifest.permission.CAMERA},1);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }else{
                    dispatchTakePictureIntent();
                }
                return true;
            case R.id.cancel:
                finish();
                System.exit(0);
                return true;
            default:
                return false;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
        }
    }

    private class AsynCaller extends AsyncTask<Void,Void,String>{

        @Override
        protected String doInBackground(Void... voids) {
            FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                    .setCloudModelName("graph")
                    .setLocalModelName("my_local_model")
                    .build();
            try {
                FirebaseModelInterpreter firebaseInterpreter =
                        FirebaseModelInterpreter.getInstance(options);

                FirebaseModelInputOutputOptions inputOutputOptions =
                        new FirebaseModelInputOutputOptions.Builder()
                                .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                                .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 5})
                                .build();
                int batchNum = 0;
                float[][][][] input = new float[1][224][224][3];
                for (int x = 0; x < 224; x++) {
                    for (int y = 0; y < 224; y++) {
                        int pixel = imageBitmap.getPixel(x, y);
                        // Normalize channel values to [0.0, 1.0]. This requirement varies by
                        // model. For example, some models might require values to be normalized
                        // to the range [-1.0, 1.0] instead.
                        input[batchNum][x][y][0] = Color.red(pixel) / 255.0f;
                        input[batchNum][x][y][1] = Color.green(pixel) / 255.0f;
                        input[batchNum][x][y][2] = Color.blue(pixel) / 255.0f;
                    }
                }

                FirebaseModelInputs inputs = null;


                inputs = new FirebaseModelInputs.Builder()
                        .add(input)  // add() as many input arrays as your model requires
                        .build();
                firebaseInterpreter.run(inputs, inputOutputOptions)
                        .addOnSuccessListener(
                                new OnSuccessListener<FirebaseModelOutputs>() {
                                    @Override
                                    public void onSuccess(FirebaseModelOutputs result) {
                                        // ...
                                        float[][] output = result.getOutput(0);
                                        float[] probabilities = output[0];
                                        BufferedReader reader = null;
                                        try {
                                            reader = new BufferedReader(
                                                    new InputStreamReader(getAssets().open("retrained_labels.txt")));
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
                                            android.widget.Toast.makeText(ImageLabellingActivity.this,label, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });


            } catch (FirebaseMLException e) {
                e.printStackTrace();
            }
            return null;
        }


    }

}
