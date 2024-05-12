package com.example.bwpic;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.otaliastudios.cameraview.PictureResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CropImage extends AppCompatActivity {

    public static PictureResult pictureResult;
    public static Bitmap croppedBitmap;

    private static SelectableImageView imageView;
    private static Bitmap originalBitmap;
    public static boolean selectionMade = false;
    private static Rect roiRect;
    private ImageButton cropBT, downloadBt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        imageView = findViewById(R.id.imageView);
        downloadBt = findViewById(R.id.downloadBt);
        cropBT = findViewById(R.id.cropBt);

        setOriginalBitmap();
        cropBT.setOnClickListener(v -> Reset());
        downloadBt.setOnClickListener(v -> download());
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


    public void setOriginalBitmap() {
        try {
            pictureResult.toBitmap(bitmap -> originalBitmap = bitmap);
            imageView.setImageBitmap(originalBitmap);
        } catch (UnsupportedOperationException e) {
            imageView.setImageDrawable(new ColorDrawable(Color.GREEN));
            Toast.makeText(this, "Can't preview this format: " + pictureResult.getFormat(), Toast.LENGTH_LONG).show();
        }
    }

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

    public static void init() {
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
            if ((roiRect.left - roiRect.right >= 50 || roiRect.right - roiRect.left >= 50) &&
                    (roiRect.top - roiRect.bottom >= 50 || roiRect.bottom - roiRect.top >= 50)) {


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

