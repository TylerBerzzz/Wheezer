package com.xperia64.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "BluetoothApp";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mInitalized = false;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private boolean mConnected = false;

    private BluetoothDevice mDevice;

    private Handler mHandler;
    private boolean mFound;

    private final int REQUEST_ENABLE_BT = 100;

    private BluetoothLeScanner mLEScanner;


    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    float xCoordinate = 0.0f; // The current x coordinate of both charts, in seconds
    float oldVal = 0.0f; // The previous data point for Integral purposes
    float oldInt = 0.0f; // The total integral value
    final float STEPSIZE = 0.140f; // This is how long it takes between receiving samples
    private final BluetoothGattCallback mGattCallback =

            new BluetoothGattCallback() {

                float fromByteArray(byte[] bytes)
                {
                    return Float.intBitsToFloat((bytes[3]&0xFF) << 24 | (bytes[2] & 0xFF) << 16 | (bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
                {
                    float f = fromByteArray(characteristic.getValue());
                    data.addEntry(new Entry(xCoordinate +=STEPSIZE, f), 0);
                    data.notifyDataChanged();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            chart.notifyDataSetChanged();
                            chart.setVisibleXRangeMaximum(6.0f);
                            chart.moveViewToX(xCoordinate +STEPSIZE);
                            chart.invalidate();
                        }
                    });
                    // Use the trapezoid method to calculate the integral
                    oldInt += STEPSIZE*(f+oldVal)/2.0f;
                    data2.addEntry(new Entry(xCoordinate +=STEPSIZE, oldInt), 0);
                    data2.notifyDataChanged();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            chart2.notifyDataSetChanged();
                            chart2.setVisibleXRangeMaximum(6.0f);
                            chart2.moveViewToX(xCoordinate +STEPSIZE);
                            chart2.invalidate();
                        }
                    });
                    oldVal = f;
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    for (BluetoothGattService service : gatt.getServices())
                    {
                        if(service.getUuid().toString().equals("19b10000-e8f2-537e-4f6c-d104768a1214"))
                        {
                            for(BluetoothGattCharacteristic characteristic : service.getCharacteristics())
                            {
                                if(characteristic.getUuid().toString().equals("19b10000-e8f2-537e-4f6c-d104768a1214"))
                                {
                                    mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor((UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")));
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mBluetoothGatt.writeDescriptor(descriptor);
                                    break;
                                }
                            }
                        }
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }
            };
    private final ScanCallback mScannerCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String devName = result.getDevice().getName();

            System.out.println("Found bluetooth device: "+devName);
            if(devName != null && devName.equals("Lungs"))
            {
                mFound = true;
                mDevice = result.getDevice();
                mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
                scanLeDevice(false);
            }
        }
    };

    public void beginBle(View v)
    {
        if(mInitalized)
            return;
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mInitalized = true;
    }

    public void scanBle(View v)
    {
        if(!mScanning && !mFound)
            scanLeDevice(true);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG, stringBuilder.toString());
            }
    }

    LineChart chart;
    LineChart chart2;
    LineDataSet dataSet;
    LineDataSet dataSet2;
    LineData data;
    LineData data2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mInitalized = false;
        mFound = false;
        chart = (LineChart) findViewById(R.id.chart);
        chart2 = (LineChart) findViewById(R.id.chart2);
        ArrayList<Entry> blankChartData1 = new ArrayList<>();
        blankChartData1.add(new Entry(0,0));
        dataSet =  new LineDataSet(blankChartData1,"Flow Rate (L/s)");
        dataSet.setColor(0xFF0072BD);
        dataSet.setCircleColor(0xFF0072BD);
        dataSet.setCircleRadius(5);
        dataSet.setDrawValues(false);
        data = new LineData(dataSet);
        chart.setData(data);
        chart.invalidate();
        ArrayList<Entry> blankChartData2 = new ArrayList<>();
        blankChartData2.add(new Entry(0,0));
        dataSet2 =  new LineDataSet(blankChartData2,"Volume (L)");
        dataSet2.setColor(0xFFD95319);
        dataSet2.setCircleColor(0xFFD95319);
        dataSet2.setCircleRadius(5);
        dataSet2.setDrawValues(false);
        data2 = new LineData(dataSet2);
        chart2.setData(data2);
        chart2.invalidate();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQUEST_ENABLE_BT) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            {
                Toast.makeText(this, "Cannot proceed without bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                mInitalized = true;
            }
        }
    }

    private static boolean mScanning;

    private static final long SCAN_PERIOD = 10000;
    private void scanLeDevice(final boolean enable) {
        if(!mInitalized)
        {
            Toast.makeText(this, "Please initialize first", Toast.LENGTH_SHORT).show();
        }
        if (enable) {
            mHandler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    mScanning = false;
                    mLEScanner.stopScan(mScannerCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLEScanner.startScan(mScannerCallback);
        } else {
            mScanning = false;
            mLEScanner.stopScan(mScannerCallback);
        }
    }
}
