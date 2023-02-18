package com.example.colorobjectdetection;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";

    private Mat mRgba;
    private ColorBlobDetector mDetector;
    private Scalar CONTOUR_COLOR;

    private CameraBridgeViewBase openCvCameraView;

    private Iterator<MatOfPoint> it;
    private double h = 15.5; // Object height
    private double f = 126.125; // Camera focal length
    private double fh = f * h;
    private double distance, accumDist = 0;

    private double bh = 0, height = 0;

    MatOfPoint contour, bcontour;

    private Moments moments;

    int cx = -1, oldCx = -1, inc = 0;

    String address;
    private ProgressDialog progress;

//    BluetoothAdapter myBluetooth = null;
//    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                openCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public void ColorObjectDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FULLSCREEN MODE
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent newint = getIntent();
//        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

//        new ConnectBT().execute(); //Call the class to connect

        setContentView(R.layout.activity_main);

        openCvCameraView = findViewById(R.id.java_camera_view);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (btSocket != null) //If the btSocket is busy
//        {
//            try {
//                btSocket.close(); //close connection
//            } catch (IOException e) {
//                msg("Error");
//            }
//        }

        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "Opencv successfully loaded");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "Opencv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallBack);
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        Mat mSpectrum = new Mat();
        Scalar mBlobColorHsv = new Scalar(29.0, 211.0, 168.0, 0.0); // Color that you want to detect
        Size SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        mDetector.process(mRgba);
        List<MatOfPoint> contours = mDetector.getContours();

        //Send command to robot

//        try {

            if (contours.size() > 0) {

                it = contours.iterator();

                while (it.hasNext()) {
                    contour = it.next();
                    height = contour.height();
                    if (bh < height) {
                        bh = height;
                        bcontour = contour;
                    }
                }

                distance = fh / bh;

                moments = Imgproc.moments(bcontour);

                oldCx = cx;
                cx = (int) (moments.get_m10() / moments.get_m00());

                accumDist = accumDist + distance;
                bh = 0;
                inc++;

                if (inc >= 18) {

                    distance = accumDist / inc;
                    Log.i(TAG, "d: " + distance);

                    accumDist = 0;
                    inc = 0;

//                    if (distance >= 21) {
//
//                        if (cx <= 700) {
//
//                            btSocket.getOutputStream().write("F:".getBytes());
//                        } else
//
//                            btSocket.getOutputStream().write("R:".getBytes());
//                    } else
//
//                        btSocket.getOutputStream().write("S:".getBytes());
                }

            } /*else if (cx > oldCx) {

                btSocket.getOutputStream().write("R:".getBytes());
            } else

                btSocket.getOutputStream().write("L:".getBytes());*/

//        } catch (IOException e) {
//            Log.e(TAG, "Error: Command not sended to robot");
//        }

        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

        return mRgba;
    }

//    @SuppressLint("StaticFieldLeak")
//    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
//    {
//        private boolean ConnectSuccess = true; //if it's here, it's almost connected
//
//        @Override
//        protected void onPreExecute() {
//            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
//        }
//
//        @Override
//        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
//        {
//            try {
//                if (btSocket == null || !isBtConnected) {
//                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
//                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
//                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        // TODO: Consider calling
//                        //    ActivityCompat#requestPermissions
//                        // here to request the missing permissions, and then overriding
//                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                        //                                          int[] grantResults)
//                        // to handle the case where the user grants the permission. See the documentation
//                        // for ActivityCompat#requestPermissions for more details.
//                        Void TODO = null;
//                        return TODO;
//                    }
//                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
//                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
//                    btSocket.connect();//start connection
//                }
//            }
//            catch (IOException e)
//            {
//                ConnectSuccess = false;//if the try failed, you can check the exception here
//            }
//            return null;
//        }
//        @Override
//        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
//        {
//            super.onPostExecute(result);
//
//            if (!ConnectSuccess)
//            {
//                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
////                finish();
//            }
//            else
//            {
//                msg("Connected.");
//                isBtConnected = true;
//            }
//            progress.dismiss();
//        }
//    }
}
