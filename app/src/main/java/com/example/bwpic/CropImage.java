package com.example.bwpic;

import static android.app.ProgressDialog.show;
import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.otaliastudios.cameraview.PictureResult;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
//import org.opencv.core.Rect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class CropImage extends AppCompatActivity {

    public static PictureResult pictureResult;
    public static Bitmap croppedBitmap;
    static Mat originalMat;
    static Mat croppedMat;

    private static SelectableImageView imageView;
    private static Bitmap originalBitmap;
    public static boolean selectionMade = false;
    private static Rect roiRect;
    private ImageButton cropBT, downloadBt;
    private ImageView close;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        if(OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV Loading Success");
            Toast.makeText(this, "OpenCV Loading Success", Toast.LENGTH_SHORT).show();
        }
        else{
            Log.e(TAG, "OpenCV Loading Error");
            Toast.makeText(this, "Error Loading OpenCV", Toast.LENGTH_SHORT).show();
        }

//        Intent intent=new Intent() ;
//        originalBitmap = intent.getParcelableExtra("bitmapImage");
        try {
            pictureResult.toBitmap(bitmap -> originalBitmap=bitmap);
            Toast.makeText(this,"Bitmap conversion success",Toast.LENGTH_LONG).show();
        } catch ( UnsupportedOperationException e) {
            Log.d("TAG", "error on setting bitmap");
        }


        imageView = findViewById(R.id.imageView);
        downloadBt = findViewById(R.id.downloadBt);
        cropBT = findViewById(R.id.cropBt);
        close = findViewById(R.id.close);

        imageView.setImageBitmap(originalBitmap);
        cropBT.setOnClickListener(v -> Reset());
        downloadBt.setOnClickListener(v -> download());
        close.setOnClickListener(v -> closeActivity());
    }

    private void closeActivity() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void download() {
        StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
        StorageVolume storageVolume = storageManager.getStorageVolumes().get(0); // internal Storage
        File fileImage = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            fileImage = new File(storageVolume.getDirectory().getPath() + "/Download/"
                    + System.currentTimeMillis()
                    + ".jpeg");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (croppedBitmap != null) {
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        } else {
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        }
        byte[] bytesArray = byteArrayOutputStream.toByteArray();
        try {

            FileOutputStream fileOutputStream = new FileOutputStream(fileImage);
            fileOutputStream.write(bytesArray);
            fileOutputStream.close();
            Toast.makeText(this, "Image saved sucessfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


//    public static void setOriginalBitmap() {
//        try {
//            pictureResult.toBitmap(bitmap -> originalBitmap = bitmap);
//            imageView.setImageBitmap(originalBitmap);
//        } catch (UnsupportedOperationException e) {
//            imageView.setImageDrawable(new ColorDrawable(Color.GREEN));
//            Toast.makeText(imageView.getContext(), "Can't preview this format: " + pictureResult.getFormat(), Toast.LENGTH_LONG).show();
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pictureResult = null;
            originalBitmap = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        pictureResult = null;
        originalBitmap = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    public void init() {
        imageView.setImageBitmap(originalBitmap);
    }

    public static void Reset() {
        imageView.setImageBitmap(originalBitmap);
        selectionMade = false;
        roiRect = null;
    }

    public static class SelectableImageView extends AppCompatImageView {

        private boolean isSelecting = false;

        private float startX, startY, endX, endY;
        private Paint paint;
        private Bitmap bitmapOverlay;


        public SelectableImageView(Context context) {
            super(context);
            init();
        }

        public SelectableImageView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setAlpha(128); // Set alpha value for the rectangle
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Draw the overlay bitmap with the rectangle
            if (bitmapOverlay != null) {
                canvas.drawBitmap(bitmapOverlay, 0, 0, null);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (selectionMade) {
                return super.onTouchEvent(event);
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isSelecting = true;
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    endX = event.getX();
                    endY = event.getY();
                    updateOverlay(); // Update the overlay bitmap
                    invalidate(); // force redraw
                    break;
                case MotionEvent.ACTION_UP:
                    isSelecting = false;
                    endX = event.getX();
                    endY = event.getY();
                    updateOverlay(); // Update the overlay bitmap
                    invalidate(); // force redraw
                    // Crop the image based on the selected ROI
                    roiRect = getRoiRect();
                    Bitmap croppedBitmap = cropImage(roiRect);
                    // Display or further process the cropped image as needed
                    if (croppedBitmap != originalBitmap) {
                        setImageBitmap(croppedBitmap);
                        selectionMade = true;
                    }
                    break;

            }
            return true;
        }

        private void updateOverlay() {
            // Create a bitmap for the overlay with the same dimensions as the original bitmap
            bitmapOverlay = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapOverlay);
            // Draw the original bitmap
            canvas.drawBitmap(originalBitmap, 0, 0, null);
            // Draw a transparent overlay using PorterDuffXfermode
            Paint transparentPaint = new Paint();
            transparentPaint.setColor(Color.TRANSPARENT);
            transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawRect(0, 0, getWidth(), getHeight(), transparentPaint);
            // Draw the rectangle on the overlay
            if (isSelecting) {
                // Ensure that y + height does not exceed bitmap height
                float adjustedEndY = Math.min(endY, getHeight() * originalBitmap.getHeight() / (float) getHeight());
                canvas.drawRect(startX, startY, endX, adjustedEndY, paint);
            }
        }

        public Rect getRoiRect() {
            // Convert touch coordinates to image coordinates
            if (roiRect == null) {
                float scaleX = (float) originalBitmap.getWidth() / getWidth();
                float scaleY = (float) originalBitmap.getHeight() / getHeight();
                int left = (int) (startX * scaleX);
                int top = (int) (startY * scaleY);
                int right = (int) (endX * scaleX);
                int bottom = (int) (endY * scaleY);
                roiRect = new Rect(left, top, right, bottom);
            }
            return roiRect;
        }

        public boolean isSelectionMade() {
            return selectionMade;
        }

        private Bitmap cropImage(Rect roiRect) {
//            if ((roiRect.x - roiRect.width >= 50 || roiRect.width - roiRect.x >= 50) &&
//                    (roiRect.y - roiRect.height >= 50 || roiRect.height - roiRect.y >= 50)) {
                if ((roiRect.left - roiRect.right >= 50 || roiRect.right - roiRect.left >= 50) &&
                        (roiRect.top - roiRect.bottom >= 50 || roiRect.bottom - roiRect.top >= 50)) {
//                if (originalBitmap!=null){
//                Utils.bitmapToMat(originalBitmap, originalMat);
//                }else{
//                    Reset();
//                }
//                croppedMat = new Mat(originalMat,roiRect);
//                Utils.matToBitmap(croppedMat,croppedBitmap);
                croppedBitmap = Bitmap.createBitmap(originalBitmap, roiRect.left, roiRect.top, roiRect.width(), roiRect.height());
            } else {
                croppedBitmap = originalBitmap;
                selectionMade = false;
                Toast.makeText(getContext(), "Please select larger area", Toast.LENGTH_SHORT).show();
                Reset();
            }
            return croppedBitmap;
        }
    }
}

