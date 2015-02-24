package com.ebookfrenzy.bluelightproto;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class BlueLightProtoActivity extends ActionBarActivity {

    public static final String TAG = "BlueLightProto";
    private BluetoothAdapter BTAdapter;
    private ArrayAdapter adapter;
    private ListView listview;
    private ToggleButton togglebutton;
    private BluetoothSocket BTSckt = null;
    private OutputStream Out = null;
    private InputStream In = null;
    private static final int EnableBT = 1;
    private static final int DiscoverBT = 2;
    private static final int DiscDur = 300;
    private final UUID SecureUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //why must this UUID?
    private final UUID UnsecureUUID = UUID.fromString("fde1b057-5906-4d22-88c5-a0412a0c758e");

    //TextView statText = (TextView)findViewById(R.id.StatText);

    private final BroadcastReceiver BcRcv = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice BTDvc = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                adapter.add(BTDvc.getName() + "\n" + BTDvc.getAddress());
            }
        }
    };

    /*
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    registerReceiver(mReceiver, filter);
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "OnCreate1" );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_light_proto);

        togglebutton = (ToggleButton)findViewById(R.id.toggleButton);
        listview = (ListView)findViewById(R.id.listView);
        Button btnOn = (Button)findViewById(R.id.OnBtn);
        Button btnOff = (Button)findViewById(R.id.OffBtn);
        SeekBar seekbar = (SeekBar)findViewById(R.id.seekBar);
        TextView intText = (TextView)findViewById(R.id.IntText);
        intText.setText(seekbar.getProgress() + " / " + seekbar.getMax());

        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Button On Pressed!");
                sendData("1");
                Log.i(TAG, "Data sent");
                toast("You have clicked ON");   //MUST FIND WAY TO GET FEEDBACK FROM ARDUINO!!!!
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Button Off Pressed!");
                sendData("2");
                toast("You have clicked OFF");   //MUST FIND WAY TO GET FEEDBACK FROM ARDUINO!!!!
            }
        });

        seekbar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    TextView intText = (TextView)findViewById(R.id.IntText);

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int x = 1;
                        int i = 0;
                        while(fromUser && i<x) {
                            intText.setText(seekBar.getProgress() + "/" + seekBar.getMax());
                            i++;
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );

        BTAdapter = BluetoothAdapter.getDefaultAdapter();

        if(BTAdapter == null) {
            ExitNoBluetooth();
            return;
        }

        listview.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.i(TAG, "onItemClick");
                        String itemValue = (String) listview.getItemAtPosition(position);
                        String MAC = itemValue.substring(itemValue.length() - 17);
                        BluetoothDevice BTDvc = BTAdapter.getRemoteDevice(MAC);

                        ConnectThread C = new ConnectThread(BTDvc);
                        Log.i(TAG, "ConnectThread ran");
                        C.start();
                        Log.i(TAG, "ConnectThread started");
                    }
                }
        );

        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1);
        listview.setAdapter(adapter);

        //adapter.add("just a test");
        Log.i(TAG, "OnCreate2" );
    }

    public void onToggleClicked(View view) {

        Log.i(TAG, "onToggleClicked1");
        adapter.clear();
        ToggleButton togglebutton = (ToggleButton) view;
        Log.i(TAG, "onToggleClicked2");

        if(togglebutton.isChecked()) {
            if(!BTAdapter.isEnabled()) {
                Log.i(TAG, "onToggleClicked, request enable BT");
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, EnableBT);
            } else {
                toast("Your bluetooth is already enabled");
                addQuery();
                discoverDevices();
                makeDiscoverable();

                AcceptThread A = new AcceptThread();
                Log.i(TAG, "AcceptThread ran");
                A.start();
                Log.i(TAG, "AcceptThread started");
            }
        } else {
            Log.i(TAG, "onToggleClicked, disabling BT");
            BTAdapter.disable();
            adapter.clear();
            toast("Your device is now disabled");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent Data) {
        if(requestCode == EnableBT) {
            if(resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "onActivityResult, BT enabled");
                toast("Bluetooth has been enabled \n Scanning for Remote Bluetooth devices...");
                addQuery();
                discoverDevices();
                makeDiscoverable();

                AcceptThread A = new AcceptThread();
                Log.i(TAG, "AcceptThread ran");
                A.start();
                Log.i(TAG, "AcceptThread started");
            } else {
                Log.i(TAG, "onActivityResult, BT fail to enable");
                toast("Bluetooth failed to enable");
                togglebutton.setChecked(false);
            }
        } else if(requestCode == DiscoverBT) {
            if(resultCode == DiscDur) {
                Log.i(TAG, "onActivityResult, device discoverable");
                toast("Your device is now discoverable for " + DiscDur + "seconds");
            } else {
                toast("Fail to enable discoverability");
            }
        }
    }

    public void addQuery() {
        Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            Log.i(TAG, "addQuery3" );
            for(BluetoothDevice device : pairedDevices) {
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    public void discoverDevices(){
        Log.i(TAG, "discoverDevices");
        if (BTAdapter.startDiscovery()) {
            toast("Discovering other bluetooth devices");
        } else {
            toast("Discovery failed to start");
        }
    }

    public void makeDiscoverable() {
        Log.i(TAG, "makeDiscoverable");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DiscDur);
        startActivityForResult(discoverableIntent, DiscoverBT);
    }

    //connect as server
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket BTSvrSckt;

        public AcceptThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = BTAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), SecureUUID);
            } catch (IOException e) {}
            BTSvrSckt = temp;
        }

        public void run() {
            TestThread("Inside AcceptThread run() 1");
            BluetoothSocket BTSckt;
            TestThread("Inside AcceptThread run() A");
            while (true) {
                try {
                    TestThread("Inside AcceptThread run() B");
                    BTSckt = BTSvrSckt.accept();
                    TestThread("Inside AcceptThread run() C");
                } catch (IOException e) {
                    e.printStackTrace();
                    TestThread("Inside AcceptThread run() D");
                    break;
                }
                if (BTSckt != null) {
                    //manageConnectedSocket(BTSckt); //Do work to manage the connection in a separate thread
                    TestThread("Inside AcceptThread run() E");

                    try {
                        TestThread("Inside AcceptThread run() F");
                        BTSvrSckt.close();
                    } catch (IOException e) {
                        TestThread("Inside AcceptThread run() G");
                        break;
                    }
                }
            }
            TestThread("Inside AcceptThread run() 2");
        }

        public void cancel() {
            try {
                BTSvrSckt.close();
            } catch (IOException e) {}
        }
    }

    //Connect as Client
    private class ConnectThread extends Thread {
        //private final BluetoothSocket BTSckt;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket temp = null;

            try {
                temp = device.createRfcommSocketToServiceRecord(SecureUUID);
            } catch (IOException e) {}
            BTSckt = temp;
        }

        public void run() {
            TestThread("Inside ConnectThread run() 1");
            BTAdapter.cancelDiscovery();
            TestThread("Inside ConnectThread run() A");
            try {
                TestThread("Inside ConnectThread run() B");
                BTSckt.connect();
                TestThread("Inside ConnectThread run() C");
            } catch (IOException connectException) {
                try {
                    TestThread("Inside ConnectThread run() D");
                    BTSckt.close();
                    TestThread("Inside ConnectThread run() E");
                } catch (IOException closeException) {}
                TestThread("Inside ConnectThread run() F");
                return;
            }

            try{
                Out = BTSckt.getOutputStream();
            } catch (IOException e) {}
            //manageConnectedSocket(BTSckt)

            TestThread("Inside ConnectThread run() 2");
        }

        public void cancel() {
            try {
                //TestThread("Inside ConnectThread run() D");

                BTSckt.close();
            } catch (IOException e) {}
        }
    }

    private void sendData(String message) {
        byte[] msg = message.getBytes();
        Log.i(TAG, "...Sending data: " + message + "...");

        try {
            Log.i(TAG, "Before Write Out");
            Out.write(msg);
            Log.i(TAG, "After Write Out");
        } catch (IOException e) {
            Log.i(TAG, "Fail to write");
            Log.i(TAG, String.valueOf(e));
        }
    }

    /*
    private class CommunicateThread extends Thread {
        private final BluetoothSocket BTSckt;
        private final InputStream In;
        private final OutputStream Out;

        public CommunicateThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {}

            In = tmpIn;
            Out = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024]; //buffer store for the stream
            int bytes; // bytes returned from read()

            while(true) {
                try {
                    bytes = In.read(buffer);
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {}
                break;
            }
        }

        public void write(byte[] bytes) {
            try {
                Out.write(bytes);
            } catch (IOException e) {}
        }

        public void cancel() {
            try {
                BTSckt.close();
            } catch (IOException e) {}
        }
    }
    */

    public void toast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    public void ExitNoBluetooth() {
        Log.i(TAG, "ExitNoBluetooth");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Cannot use BlueLightProto without bluetooth")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("BlueLightProto")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        builder.create().show();
    }

    public void TestThread(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                Log.i(TAG, text);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.i(TAG, "onResume before register");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(BcRcv, filter);
        //Log.i(TAG, "onResume after register");

        //Log.i(TAG, "onResume before getOutputStream");
        /*while (true) {
            if (BTSckt != null) {
                try {
                    Out = BTSckt.getOutputStream();
                } catch (IOException e) {
                    Log.i(TAG, "onResume fail to get OutputStream");
                }
                Log.i(TAG, "onResume after getOutputStream");
            } else {
                Log.i(TAG, "onResume BTSckt is null");
                break;
            }
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.i(TAG, "onPause before unregister");
        this.unregisterReceiver(BcRcv);
        //Log.i(TAG, "onPause after unregister");

        Log.i(TAG, "onPause before flush");
        if (Out != null) {
            try{
                Out.flush();
            } catch (IOException e) {}
        }
        Log.i(TAG, "onPause after flush");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_blue_light_proto, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
