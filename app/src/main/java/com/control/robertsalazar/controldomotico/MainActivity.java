package com.control.robertsalazar.controldomotico;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends ActionBarActivity{

    //Eventos en el LOG
    private static final boolean D = true   ;
    private static final String TAG = "ControlDomotico";

    //intents
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;


    //Variables Bluetooth
    private BluetoothSocket btSocket;
    private Button btToggle;
    private ConnectAsyncTask connectAsyncTask;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private Handler mHandler;
    private int mInterval = 35;
    private boolean si_estoy_conectado=false;

    int miLuz1='M';
    //Botones, checkbox y demás

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mHandler = new Handler();

        final CheckBox luz1 = (CheckBox) findViewById(R.id.luz1);
        final CheckBox luz2 = (CheckBox) findViewById(R.id.luz2);
        final CheckBox conectado = (CheckBox) findViewById(R.id.conectado);



        btToggle = (Button) findViewById(R.id.button);
        btToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(luz1.isChecked()){
                    if(D)Log.d(TAG,"luz1 true");
                    miLuz1='L';
                }
                else {
                    if(D)Log.d(TAG,"luz1 false");
                    miLuz1='M';
                }
                if(luz2.isChecked()){
                    if(D)Log.d(TAG,"luz1 true");
                }
                else {
                    if (D) Log.d(TAG, "luz1 false");
                }
                if(conectado.isChecked()) {
                    si_estoy_conectado=true;
                    if(D) Log.e(TAG, "si cambia la variable");
                }
                else
                    si_estoy_conectado=false;

            }
        });

        if(mBluetoothAdapter == null){
            //Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "No está disponible bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        startRepeatingTask();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
             Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }
        else {
            setUpControlDomotico();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        stopRepeatingTask();
    }

    private void setUpControlDomotico(){
        connectAsyncTask = new ConnectAsyncTask();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setUpControlDomotico();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        connectAsyncTask.execute(device);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // MenuInflater inflater = getMenuInflater();
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.Conexion:
                // Muestra la lista de dispositivos disponibles.
                if(D) Log.e(TAG, "Se presionó");
                 serverIntent = new Intent(this, DeviceListActivity.class);
                if(D) Log.e(TAG, "Server intent");
                 startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                if(D) Log.e(TAG, "Se envío?");
                return true;
        }
        return false;
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus(); //this function can change value of mInterval.
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    private void updateStatus() {

        OutputStream mmOutStream = null;
        //InputStream mmInStream = null;
        if(si_estoy_conectado) {
            //if(D) Log.e(TAG, Integer.toString(miLuz1));
            try {
                mmOutStream = btSocket.getOutputStream();
                mmOutStream.write('B');
                mmOutStream.write(miLuz1);
            } catch (IOException e) {}
            try {
                mmOutStream = btSocket.getOutputStream();
                mmOutStream.write(0xD);
                mmOutStream.write(0xA);
            } catch (IOException e) {
            }
        }
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }


    private class ConnectAsyncTask extends AsyncTask<BluetoothDevice, Integer, BluetoothSocket>{
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;


        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... device) {

            mmDevice = device[0];

            try {
                mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                mmSocket.connect();

            } catch (Exception e) { }

            return mmSocket;
        }
        @Override
        protected void onPostExecute(BluetoothSocket result) {

            btSocket = result;
            //Enable Button
            btToggle.setEnabled(true);

        }
    }
}



