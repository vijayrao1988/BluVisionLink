package com.bluetooth.bdsk.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import com.bluetooth.bdsk.Constants;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blitz on 10/07/17.
 */

public class BleScanner {
    private BluetoothLeScanner scanner = null;
    private BluetoothAdapter bluetooth_adapter = null;
    private Handler handler = new Handler();
    private ScanResultsConsumer scan_results_consumer;
    private Context context;
    private boolean scanning = false;
    private String device_name_start = "";

    public BleScanner(Context context) {
        this.context = context;

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        bluetooth_adapter = bluetoothManager.getAdapter();

        //check bluetooth is available and on
        if (bluetooth_adapter == null || !bluetooth_adapter.isEnabled()) {
            Log.d(Constants.TAG, "Bluetooth is NOT switched on");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }
        Log.d(Constants.TAG, "Bluetooth is switched on");
    }

    public void startScanning(final ScanResultsConsumer scan_results_consumer, long stop_after_ms) {
        if (scanning) {
            Log.d(Constants.TAG, "Already scanning so ignoring startScanning request");
            return;
        }
        if (scanner == null) {
            scanner = bluetooth_adapter.getBluetoothLeScanner();
            Log.d(Constants.TAG, "Created BluetoothScanner object");
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    Log.d(Constants.TAG, "Scan stopped");
                    scanner.stopScan(scan_callback);
                    setScanning(false);
                }
            }
        }, stop_after_ms);

        this.scan_results_consumer = scan_results_consumer;
        Log.d(Constants.TAG, "Scanning");
        List<ScanFilter> filters;

        filters = new ArrayList<ScanFilter>();
        //ScanFilter filter = new ScanFilter.Builder().setDeviceName("Pebble").build();
        //filters.add(filter);

        ScanSettings settings = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0)
                    .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                    .build();
        }
        setScanning(true);

        scanner.startScan(filters, settings, scan_callback);
    }

    public void stopScanning() {
        setScanning(false);
        Log.d(Constants.TAG, "Scan stopped");
        scanner.stopScan(scan_callback);
    }

    private ScanCallback scan_callback = new ScanCallback() {
        public void onScanResult(int callbackType, final ScanResult result) {
            if (!scanning) {
                return;
            }
            scan_results_consumer.candidateBleDevice(result, result.getRssi());
        }
    };

    public boolean isScanning() {
        return scanning;
    }

    void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (!scanning) {
            scan_results_consumer.scanningStopped();
        } else {
            scan_results_consumer.scanningStarted();
        }
    }


    //end of class
}
