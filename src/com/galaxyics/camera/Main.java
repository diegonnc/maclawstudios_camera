/************************************************************************/
/* MaclawStudios Camera App for Samsung Galaxy Ace and Gio              */
/* Copyright (C) 2012 Marcin Chojnacki & MaclawStudios                  */
/*                                                                      */
/* This program is free software: you can redistribute it and/or modify */
/* it under the terms of the GNU General Public License as published by */
/* the Free Software Foundation, either version 3 of the License, or    */
/* (at your option) any later version.                                  */
/*                                                                      */
/* This program is distributed in the hope that it will be useful,      */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of       */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the         */
/* GNU General Public License for more details.                         */
/*                                                                      */
/* You should have received a copy of the GNU General Public License    */
/* along with this program.  If not, see <http://www.gnu.org/licenses/> */
/************************************************************************/

package com.galaxyics.camera;

import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class Main extends Activity implements Camera.PreviewCallback, SensorEventListener {
    static {
        System.loadLibrary("nativeyuv");
    }
    
    private native void nativeYUV(byte[] yuv,int[] rgb,int width,int height);
    
    private SensorManager sManager;
    private Camera mCamera;
    private Bitmap mBitmap;
    private int[] buffer;
    private int orient;
    
    private int alpha;
    public static final String tag="Camera";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        sManager=(SensorManager)getSystemService(SENSOR_SERVICE);  
        sManager.registerListener(this,sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        mBitmap=Bitmap.createBitmap(320,240,Config.RGB_565);
        buffer=new int[320*240];

        while(true) {
            boolean caught=false;
            try {
                Log.i(tag,"Trying to start camera HAL...");
                mCamera=Camera.open();
            }
            catch(Exception e) {
                Log.i(tag,"Failed to start camera HAL, waiting 50ms and retrying...");
                try { Thread.sleep(50); } catch (InterruptedException f) { }
                caught=true;
            }
            if(!caught) break;
        }
        
        Camera.Parameters parameters=mCamera.getParameters();
        String model=Build.MODEL;
        
        if(model.compareTo("GT-S5830")==0) {
            parameters.setPictureSize(2560,1920);
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }
        else if(model.compareTo("GT-S5660")==0) parameters.setPictureSize(2048,1536);
        else {
            Toast.makeText(getApplicationContext(),getString(R.string.unsupported),Toast.LENGTH_LONG).show();
            onStop();
            return;
        }
        
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(parameters);
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Toast.makeText(getApplicationContext(),getString(R.string.hello),Toast.LENGTH_LONG).show();
    }
    
    public void onStop() {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        super.onStop();
        finish();
    }

    public void onPreviewFrame(byte[] data,Camera camera) {
        try {
            nativeYUV(data,buffer,320,240);
            mBitmap.setPixels(buffer,0,320,0,0,320,240);
            SurfaceHolder holder=((SurfaceView)findViewById(R.id.preview)).getHolder();
            Canvas canvas=holder.lockCanvas();
            canvas.scale((float)canvas.getWidth()/320,(float)canvas.getHeight()/240);
            canvas.drawBitmap(mBitmap,0,0,null);
            holder.unlockCanvasAndPost(canvas);
        }
        catch(Exception e) {
            return;
        }
    }

    public void onSnapshot(View v) {
        //((ImageView)v).setImageResource(R.drawable.button_pressed);
        Camera.PictureCallback callback=new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data,Camera camera) {
                Bitmap tmp=BitmapFactory.decodeByteArray(data,0,data.length);
                Matrix matrix=new Matrix();
                matrix.postRotate(orient);
                Bitmap rotated=Bitmap.createBitmap(tmp,0,0,tmp.getWidth(),tmp.getHeight(),matrix,true);
                try {
                    Name n=new Name();
                    String name=n.getName();
                    FileOutputStream out=new FileOutputStream(name);
                    rotated.compress(Bitmap.CompressFormat.JPEG,100,out);
                    n.scanFile(getApplicationContext(),name);            
                    mCamera.startPreview();
                }
                catch(Exception e) {
                    Toast.makeText(getApplicationContext(),getString(R.string.error),Toast.LENGTH_SHORT).show();
                }
            }
        };
        mCamera.takePicture(null,null,callback);
        blink();
        //Toast.makeText(getApplicationContext(),"Photo was captured!",Toast.LENGTH_SHORT).show();
        //((ImageView)v).setImageResource(R.drawable.button_released);
    }
    
    private void blink() {
        alpha=250;
        final Timer timer=new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SurfaceHolder holder=((SurfaceView)findViewById(R.id.preview)).getHolder();
                Canvas canvas=holder.lockCanvas();
                canvas.scale((float)canvas.getWidth()/320,(float)canvas.getHeight()/240);
                canvas.drawBitmap(mBitmap,0,0,null);
                Paint paint=new Paint();
                paint.setColor(Color.WHITE);
                paint.setAlpha(alpha);
                canvas.drawRect(0,0,320,240,paint);
                holder.unlockCanvasAndPost(canvas);
                alpha-=5;
                if(alpha==0) timer.cancel();
            }
        },10,10);
        MediaPlayer mp=MediaPlayer.create(this,R.raw.shutter);   
        mp.start();
        mp.setOnCompletionListener(new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        //do nothing
    }

    public void onSensorChanged(SensorEvent arg) {
        if(arg.accuracy!=SensorManager.SENSOR_STATUS_UNRELIABLE) {
            if(arg.values[0]<-5) orient=180;
            else if(arg.values[0]>5) orient=0;
            else orient=90;
        }
    }
}