package com.wit.example;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVWriter;
import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException;
import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;
import com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


/**
 * Function: main interface
 * Explanation：
 * 1. This program is an example base on Bluetooth 5.0sdk developed by WitMotion
 * 2. This program is applicable to the following products of WitMotion
 * BWT901BLECL5.0
 * BWT901BLE5.0
 * WT901BLE5.0
 * 3. This program has only one page and no other pages
 *
 * @author huangyajun
 * @date 2022/6/29 11:35
 */


public class MainActivity<my_mac> extends AppCompatActivity implements IBluetoothFoundObserver, IBwt901bleRecordObserver {

    /**
     * log tag
     */
    private static final String TAG = "MainActivity";

    /**
     * Device List
     */
    private List<Bwt901ble> bwt901bleList = new ArrayList<>();

    /**
     * Controls whether the auto-refresh thread works
     */
    private boolean destroyed = true;
    private FileWriter mFileWriter;

    /**
     * activity when created
     *
     * @author huangyajun
     * @date 2022/6/29 8:43
     */

    EditText editText1;
    EditText editText2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText1 = findViewById(R.id.number1);
        editText2 = findViewById(R.id.number2);

        // Initialize the Bluetooth manager, here will apply for Bluetooth permissions
        WitBluetoothManager.initInstance(this);

        // start search button
        Button startSearchButton = findViewById(R.id.startSearchButton);
        startSearchButton.setOnClickListener((v) -> {
            startDiscovery();
        });

        // stop search button
        Button stopSearchButton = findViewById(R.id.stopSearchButton);
        stopSearchButton.setOnClickListener((v) -> {
            stopDiscovery();
        });

        // Acceleration calibration button
        Button appliedCalibrationButton = findViewById(R.id.appliedCalibrationButton);
        appliedCalibrationButton.setOnClickListener((v) -> {
            handleAppliedCalibration();
        });

        // Start Magnetic Field Calibration button
        Button startFieldCalibrationButton = findViewById(R.id.startFieldCalibrationButton);
        startFieldCalibrationButton.setOnClickListener((v) -> {
            handleStartFieldCalibration();
        });

        // End Magnetic Field Calibration button
        Button endFieldCalibrationButton = findViewById(R.id.endFieldCalibrationButton);
        endFieldCalibrationButton.setOnClickListener((v) -> {
            handleEndFieldCalibration();
        });

        Instant instant = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            instant = Instant.now();
        }

        String tempo = instant.toString();
        String tempo_menor=tempo.substring(0,19);

        char[] mytempo;
        mytempo = tempo_menor.toCharArray();
        mytempo[13] = '_';
        mytempo[16] = '_';
        tempo_menor = String.valueOf(mytempo);



        fileName= "Documents/" + tempo_menor + ".csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath);

        try {
            mFileWriter = new FileWriter(filePath , true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // File exist
        if(f.exists()&&!f.isDirectory())
        {

            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        }
        else
        {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Auto refresh data thread
        thread = new Thread(this::refreshDataTh);
       // destroyed = false;
        thread.start();
    }

    /**
     * activity perish
     *
     * @author huangyajun
     * @date 2022/6/29 13:59
     */
    @Override
    protected void onDestroy() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();

    }

    /**
     * Start searching for devices
     *
     * @author huangyajun
     * @date 2022/6/29 10:04
     */
    String return_mac(int number)
    {
        if(number>=1 && number<=4)
        {
            return mac[number-1];
        }
        else
        {
            return "";
        }
    }

    public void startDiscovery() {
        WitBluetoothManager.initInstance(this);

        destroyed=true;
        n_actual=0;
        number1=0;
        number2=0;
        // Turn off all device
        runOnUiThread(() -> {
            try{
                Editable e1 = editText1.getText();
                if(e1!=null) {
                    number1 = Integer.parseInt(editText1.getText().toString());
                }
                else
                {
                    number1=0;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            try{
                Editable e2 = editText2.getText();
                if(e2!=null) {
                    number2 = Integer.parseInt(editText2.getText().toString());
                }
                else
                {
                    number2=0;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

        });

        mac_search[0]=return_mac(number1);
        mac_search[1]=return_mac(number2);
        //Log.d("Teste", mac_search[0]);
        //Log.d("Teste", mac_search[1]);

        for (int i = 0; i < bwt901bleList.size(); i++) {
            Bwt901ble bwt901ble = bwt901bleList.get(i);
            bwt901bleList.remove(i);
            bwt901ble.removeRecordObserver(this);
            bwt901ble.close();
        }

        // Erase all devices
        bwt901bleList.clear();
       // bwt901bleList.
      //  bwt901bleList = new ArrayList<>();

        // Start searching for bluetooth
        try {
            // get bluetooth manager
            WitBluetoothManager bluetoothManager = WitBluetoothManager.getInstance();
            // Monitor communication signals
            bluetoothManager.registerObserver(this);
            // start search
            bluetoothManager.startDiscovery();
        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        }
        destroyed=false;
        //thread.resume();
    }



     //   stopDiscovery();
    public void stopDiscovery() {
        // stop searching for bluetooth
        try {
            // acquire Bluetooth manager
            WitBluetoothManager bluetoothManager = WitBluetoothManager.getInstance();
            // Cancel monitor communication signals
            bluetoothManager.removeObserver(this);
            // stop searching
            bluetoothManager.stopDiscovery();
        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will be called back when a Bluetooth 5.0 device is found
     *
     * @author huangyajun
     * @date 2022/6/29 8:46
     */

    @Override
    public void onFoundBle(BluetoothBLE bluetoothBLE) {
        // Create a Bluetooth 5.0 sensor connection object
       //
        if(n_actual<N) {
            Bwt901ble bwt901ble = new Bwt901ble(bluetoothBLE);
            // add to device list

                name=bwt901ble.getDeviceName();
                my_mac[n_actual]=name;
               // Log.d("Tag", my_mac[n_actual]);
                for(int i=0;i<2;i++)
                {
                    if(mac_search[i]!="") {
                        if (my_mac[n_actual].contains(mac_search[i])) {
                            if(1==n_actual) {
                                Log.d("comp1", my_mac[0]);
                                Log.d("comp1", my_mac[1]);
                            }
                            if (n_actual == 0 || ((n_actual == 1) && (my_mac[0] != my_mac[1]))) {//
                                //numero[n_actual] = i;
                                if(n_actual == 1)
                                    Log.d("AQUI",my_mac[n_actual]);
                                n_actual = n_actual + 1;
                                bwt901bleList.add(bwt901ble);

                                // Registration data record
                                bwt901ble.registerRecordObserver(this);

                                // Turn on the device
                                try {
                                    bwt901ble.open();
                                } catch (OpenDeviceException e) {
                                    // Failed to open device
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                }

        }
    }

    /**
     * This method will be called back when a Bluetooth 2.0 device is found
     *
     * @author huangyajun
     * @date 2022/6/29 10:01
     */
    @Override
    public void onFoundSPP(BluetoothSPP bluetoothSPP) {
        // Without doing any processing, this sample program only demonstrates how to connect a Bluetooth 5.0 device
    }

    /**
     * This method will be called back when data needs to be recorded
     *
     * @author huangyajun
     * @date 2022/6/29 8:46
     */
    @Override
    public void onRecord(Bwt901ble bwt901ble) {
        int n_save=30;

        String deviceData = getDeviceData(bwt901ble);
       // Log.d(TAG,  "device data [ " + bwt901ble.getDeviceName() + "] = " + deviceData);
       // String[] data = {"Ship Name", "Scientist Name", "...", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").formatter.format(date)});
      //  String[] data = {"Ship Name", "Scientist Name"};


        if(cont_tempo==n_save) {
            Instant instant_t = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                instant_t = Instant.now();

            }
            String[] tempo = new String[]{instant_t.toString()};
          //  Log.d(TAG, filePath);
            writer.writeNext(tempo);
            writer.writeNext(new String[]{deviceData});
            // writer.writeNext(data);
            cont_tempo=0;
        }
        cont_tempo++;
    }

    /**
     *Auto refresh data thread
     *
     * @author huangyajun
     * @date 2022/6/29 13:41
     */
    Handler h = new Handler();

    final Runnable r = new Runnable() {
        public void run() {
            do_this();
            h.postDelayed(r, 100);
        }
    };

    private void do_this() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < bwt901bleList.size(); i++) {
            // Make all devices accelerometer calibrated
            Bwt901ble bwt901ble = bwt901bleList.get(i);

            String deviceData = getDeviceData2(bwt901ble);
            text.append(deviceData);
            onRecord(bwt901ble);
            //bwt901ble=null;
        }

        TextView deviceDataTextView = findViewById(R.id.deviceDataTextView);
        runOnUiThread(() -> {
            deviceDataTextView.setText(text.toString());
        });
    }

    private void refreshDataTh() {
        h.postDelayed(r, 100);

    }

    /**
     * Get a device's data
     *
     * @author huangyajun
     * @date 2022/6/29 11:37
     */
    private String getDeviceData(Bwt901ble bwt901ble) {
        StringBuilder builder = new StringBuilder();

        builder.append(bwt901ble.getDeviceName()).append("\n");
        builder.append(getString(R.string.accX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AccX)).append("g \t");
        builder.append(getString(R.string.accY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AccY)).append("g \t");
        builder.append(getString(R.string.accZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AccZ)).append("g \n");
        builder.append(getString(R.string.asX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AsX)).append("°/s \t");
        builder.append(getString(R.string.asY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AsY)).append("°/s \t");
        builder.append(getString(R.string.asZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AsZ)).append("°/s \n");
        builder.append(getString(R.string.angleX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AngleX)).append("° \t");
        builder.append(getString(R.string.angleY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AngleY)).append("° \t");
        builder.append(getString(R.string.angleZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AngleZ)).append("° \n");
        builder.append(getString(R.string.hX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.HX)).append("\t");
        builder.append(getString(R.string.hY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.HY)).append("\t");
        builder.append(getString(R.string.hZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.HZ)).append("\n");
        builder.append(getString(R.string.t)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.T)).append("\n");
        builder.append(getString(R.string.electricQuantityPercentage)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.ElectricQuantityPercentage)).append("\n");
        builder.append(getString(R.string.versionNumber)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.VersionNumber)).append("\n");
        return builder.toString();
    }

    private String getDeviceData2(Bwt901ble bwt901ble) {
        StringBuilder builder = new StringBuilder();

        int i;
        for (i = 0; i < 4; i++) {
            if (bwt901ble.getDeviceName().contains(mac[i])) {
                break;
            }
        }
        builder.append(nominho[i]).append("\n");
        builder.append(mac[i]).append("\n");

        //builder.append(getString(R.string.asX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AsX)).append("°/s \t");
        //builder.append(getString(R.string.asY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AsY)).append("°/s \t");
        //builder.append(getString(R.string.asZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AsZ)).append("°/s \n");
        builder.append(getString(R.string.angleX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AngleX)).append("° \t");
        builder.append(getString(R.string.angleY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AngleY)).append("° \t").append("\n");
        builder.append(getString(R.string.accX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AccX)).append("g \t");
        //builder.append(getString(R.string.accY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AccY)).append("g \t");
        builder.append(getString(R.string.accZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AccZ)).append("g \n");
        //builder.append(getString(R.string.angleZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.AngleZ)).append("° \n");
        //builder.append(getString(R.string.hX)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.HX)).append("\t");
        //builder.append(getString(R.string.hY)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.HY)).append("\t");
        // builder.append(getString(R.string.hZ)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.HZ)).append("\n");
        builder.append(getString(R.string.t)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.T)).append("\n");
        //builder.append(getString(R.string.electricQuantityPercentage)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.ElectricQuantityPercentage)).append("\n");
        //builder.append(getString(R.string.versionNumber)).append(":").append(bwt901ble.getDeviceData(WitSensorKey.VersionNumber)).append("\n");
        builder.append("\n");
        return builder.toString();
    }

    /**
     * Make all devices accelerometer calibrated
     *
     * @author huangyajun
     * @date 2022/6/29 10:25
     */
    private void handleAppliedCalibration() {
        for (int i = 0; i < bwt901bleList.size(); i++) {
            Bwt901ble bwt901ble = bwt901bleList.get(i);
            // unlock register
            bwt901ble.unlockReg();
            // send command
            bwt901ble.appliedCalibration();
        }
        Toast.makeText(this, "OK", Toast.LENGTH_LONG).show();
    }

    /**
     * Let all devices begin magnetic field calibration
     *
     * @author huangyajun
     * @date 2022/6/29 10:25
     */
    private void handleStartFieldCalibration() {
        for (int i = 0; i < bwt901bleList.size(); i++) {
            Bwt901ble bwt901ble = bwt901bleList.get(i);
            // unlock register
            bwt901ble.unlockReg();
            // send command
            bwt901ble.startFieldCalibration();
        }
        Toast.makeText(this, "OK", Toast.LENGTH_LONG).show();
    }

    /**
     * Let's all devices end the magnetic field calibration
     *
     * @author huangyajun
     * @date 2022/6/29 10:25
     */
    private void handleEndFieldCalibration() {
        for (int i = 0; i < bwt901bleList.size(); i++) {
            Bwt901ble bwt901ble = bwt901bleList.get(i);
            // unlock register
            bwt901ble.unlockReg();
            // send command
            bwt901ble.endFieldCalibration();
        }
        Toast.makeText(this, "OK", Toast.LENGTH_LONG).show();
    }

    String[] my_mac= {"lalalallaallalalalalfadsfasdasd","sadasdasdasdasdasdasdasdas"};//new String[2];
    String[] mac_search= {"lalalallaallalalalalfadsfasdasd","sadasdasdasdasdasdasdasdas"};

    String mac1 = "E9:C6:7F:DA:76:FA";
    String mac2 = "ED:D5:CA:4A:E4:13";
    String mac3 = "C0:8B:52:DF:49:43";
    String mac4 = "E5:CF:9B:83:FD:FE";

    String mac[] = {mac1, mac2, mac3, mac4};

    String name1 = "APARELHO 1";
    String name2 = "APARELHO 2";
    String name3 = "APARELHO 3";
    String name4 = "APARELHO 4";

    String nominho[] = {name1, name2, name3, name4};

    int[] numero=new int[2];
    int N = 2;
    int n_actual=0;
    String name;
    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    String fileName = "Documents/AnalysisData.csv";
    String filePath; //= baseDir + File.separator + fileName;
    File f;// = new File(filePath);
    CSVWriter writer;
    int cont_tempo=0;
    int number1,number2;
    Thread thread;
}