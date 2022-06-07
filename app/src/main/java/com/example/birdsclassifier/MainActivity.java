package com.example.birdsclassifier;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.birdsclassifier.ml.BirdsModel;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button mLoadImage;
    TextView mResult;
    ImageView mAddImage;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> mGetContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        IT IS IMPORTANT TO DOWNLOAD THE TENSORFLOW *LITE* FILE FROM TENSORFLOW HUB
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddImage = findViewById(R.id.addImage);
        mLoadImage = findViewById(R.id.upload_button);
        mResult = findViewById(R.id.tv_result);

        mResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + mResult.getText().toString()));
                startActivity(intent);
            }
        });


//      This is the new method to upload images from gallery (as the other way to do so is deprecated)
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                Bitmap imageBitmap = null;

                try {
                    imageBitmap = UriToBitmap(result);
                }
                catch (IOException e){
                    e.printStackTrace();
                }

                mAddImage.setImageBitmap(imageBitmap);
                outputGenerator(imageBitmap);
            }
        });

        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContent.launch("image/*");
            }
        });

    }

    private Bitmap UriToBitmap(Uri result) throws IOException {
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(), result);
    }

    private void outputGenerator(Bitmap imageBitmap){
//        The code in try and catch block is already present in BirdsModel.tflite
        try {
            BirdsModel model = BirdsModel.newInstance(MainActivity.this);

            // Creates inputs for reference.
            TensorImage image = TensorImage.fromBitmap(imageBitmap);

            // Runs model inference and gets result.
            BirdsModel.Outputs outputs = model.process(image);
            List<Category> probability = outputs.getProbabilityAsCategoryList();

//            All the images from google are added to the list. Each image has it's own probability associated with it's name. For example, each bird will have a 0 probability of being a parrot(except parrot, but there might be some lookalikes), so we get the name of that bird whose probablility of being the bird of the uploaded image is maximum.

            int index = 0;
            float max = probability.get(0).getScore();
            for (int i = 0; i< probability.size(); i++){
                if (max< probability.get(i).getScore()){
                    max = probability.get(i).getScore();
                    index = i;
                }
            }

            Category output = probability.get(index);
            mResult.setText(output.getLabel());
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }
}