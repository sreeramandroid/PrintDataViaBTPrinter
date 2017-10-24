package printer.com.printdataviabtprinter;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;


/**
 * created by shankar
 ***/

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ProgressDialog mProgressDialog;
    private Button mbtnPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mbtnPrint = (Button) findViewById(R.id.button);
        mbtnPrint.setOnClickListener(this);
    }

    private static final int REQUEST_ENABLE_BT = 2;
    BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private UUID applicationUUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    SharedPreferences sharedpreferences;


    public void showLoadingDialog(final String title, final boolean isCancelable) {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(title);
            mProgressDialog.setCancelable(isCancelable);
            mProgressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideLoadingDialog() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            mProgressDialog = null;
        } catch (Exception e) {
            mProgressDialog = null;
        }
    }


    public void printReceipt() {
        showLoadingDialog("Connecting Printer...", false);
        sharedpreferences = this.getSharedPreferences("BluetoothDevice", Context.MODE_PRIVATE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this.getApplicationContext(), "Blutooth adapter not found", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent,
                        REQUEST_ENABLE_BT);
            } else {
                String mDeviceAddress = sharedpreferences.getString("mBluetoothDevice", null);
                if (mDeviceAddress != null) {
                    try {
                        mBluetoothDevice = mBluetoothAdapter
                                .getRemoteDevice(mDeviceAddress);
                        mBluetoothAdapter.cancelDiscovery();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                            mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(applicationUUID);
                        }
                        mBluetoothSocket.connect();
                        printData();
                    } catch (IOException e) {
                        Toast.makeText(this.getApplicationContext(), "Unable to connect to printer!", Toast.LENGTH_SHORT).show();
                        displayDevicesDialog();
                        e.printStackTrace();
                    }
                } else
                    displayDevicesDialog();
            }
        }
    }

    public void onActivityResult(int mRequestCode, int mResultCode,
                                 Intent mDataIntent) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent);

        switch (mRequestCode) {
            case REQUEST_ENABLE_BT:
                if (mResultCode == Activity.RESULT_OK) {
                    String mDeviceAddress = sharedpreferences.getString("mBluetoothDevice", null);
                    if (mDeviceAddress != null)
                        try {
                            mBluetoothDevice = mBluetoothAdapter
                                    .getRemoteDevice(mDeviceAddress);
                            mBluetoothAdapter.cancelDiscovery();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                                mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(applicationUUID);
                            }
                            mBluetoothSocket.connect();
                            printData();
                        } catch (IOException e) {
                            Toast.makeText(this.getApplicationContext(), "Unable to connect to printer!", Toast.LENGTH_SHORT).show();
                            displayDevicesDialog();
                            e.printStackTrace();
                        }
                    else
                        displayDevicesDialog();
                } else {
                    Toast.makeText(this.getApplicationContext(), "Message", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void displayDevicesDialog() {
        hideLoadingDialog();
        final Dialog myDialog = new Dialog(this,
                R.style.Theme_AppCompat);
        myDialog.setContentView(R.layout.device_list);
        myDialog.show();
        myDialog.setCanceledOnTouchOutside(true);
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView mPairedListView = (ListView) myDialog.findViewById(R.id.paired_devices);
        mPairedListView.setAdapter(mPairedDevicesArrayAdapter);
        mPairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    myDialog.cancel();
                    showLoadingDialog("Connecting Printer...", false);
                    mBluetoothAdapter.cancelDiscovery();
                    String mDeviceInfo = ((TextView) view).getText().toString();
                    String mDeviceAddress = mDeviceInfo.substring(mDeviceInfo.length() - 17);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mBluetoothDevice", mDeviceAddress);
                    editor.commit();
                    mBluetoothDevice = mBluetoothAdapter
                            .getRemoteDevice(mDeviceAddress);
                    try {
                        mBluetoothAdapter.cancelDiscovery();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                            mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(applicationUUID);
                        }
                        mBluetoothSocket.connect();
                        printData();
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "Unable to connect to printer!", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } catch (Exception ex) {

                }
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> mPairedDevices = mBluetoothAdapter.getBondedDevices();

        if (mPairedDevices.size() > 0) {
            myDialog.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice mDevice : mPairedDevices) {
                mPairedDevicesArrayAdapter.add(mDevice.getName() + "\n" + mDevice.getAddress());
            }
        } else {
            String mNoDevices = "None Paired";
            mPairedDevicesArrayAdapter.add(mNoDevices);
        }
        Button clr_btn = (Button) myDialog.findViewById(R.id.btn_cancel);
        myDialog.show();
        clr_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });
    }

    public void printData() {
        Thread t = new Thread() {
            public void run() {
                try {
                    OutputStream os = mBluetoothSocket
                            .getOutputStream();
                    String PrintSting = "";

                    PrintSting = "National Highway Authority Of\n"
                            + "           India\n ";
                   /* PrintSting = PrintSting
                            + "-------------------------------\n";*/

                  /*  PrintSting = PrintSting
                            + "E ID:1023              14.09.17\n";*/
                 /*   PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting
                            + "       Highway   \n";

                   /* PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting
                            + "       Tollway Pvt. Ltd\n";
                    PrintSting = PrintSting
                            + "       Toll Plaza Name\n";
                   /* PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "(Km 38+300 On NH 163)\n"
                            + "           Section\n ";
                    PrintSting = PrintSting + "(Section Of\n"
                            + "           NH-163)\n ";
                 /*   PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "  (Km 18+600 to Km 54+000 in\n"
                            + "   The State Of -------)\n ";
/*
                    PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "Ticket No.          : 702064549\n";
                    PrintSting = PrintSting + "Booth&operator id :LANES Bnaren\n";
                   /* PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "Date & Time      :   28/09/2017\n";
                    PrintSting = PrintSting + "                       02:35:03\n";

                    PrintSting = PrintSting + "Vehicle No      :  [----------]\n";
                   /* PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "Type Of Journey :Double Journey\n";
                   /* PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "Fee                      : 125\n";
                   /* PrintSting = PrintSting
                            + "  \n";*/
                    PrintSting = PrintSting + "Only For Overloaded Vehicle    \n";

                    PrintSting = PrintSting
                            + "  \n";
                    PrintSting = PrintSting + "Standard wt. Of Vehicle  : -----\n";
                    PrintSting = PrintSting + "Actual wt. of vehicle    :\n";
                    PrintSting = PrintSting + "Overloaded Vehicles Fees :0.00\n";
                    PrintSting = PrintSting + "Total                    : -----\n";
                    PrintSting = PrintSting
                            + "  \n";
                    PrintSting = PrintSting + "WISH YOU SAFE & HAPPY JOURNEY\n";
                    PrintSting = PrintSting + "\n\n ";
                    os.write(PrintSting.getBytes());


                    int gs = 10;
                    os.write(intToByteArray(gs));
                    int h = 70;
                    os.write(intToByteArray(h));
                    int n = 102;
                    os.write(intToByteArray(n));

                    // Setting Width
                    int gs_width = 10;
                    os.write(intToByteArray(gs_width));
                    int w = 84;
                    os.write(intToByteArray(w));
                    int n_width = 2;
                    os.write(intToByteArray(n_width));


                    mBluetoothSocket.close();
                    mBluetoothSocket = null;
                    hideLoadingDialog();
                } catch (Exception e) {
                    Log.e("MainActivity", "Exe ", e);
                }
            }
        };
        t.start();
    }

    public static byte intToByteArray(int value) {
        byte[] b = ByteBuffer.allocate(1).putInt(value).array();
        for (int k = 0; k < b.length; k++) {
            System.out.println("developer  [" + k + "] = " + "0x"
                    + UnicodeFormatter.byteToHex(b[k]));
        }

        return b[2];
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                printReceipt();
                break;
            default:
                break;
        }

    }
}
