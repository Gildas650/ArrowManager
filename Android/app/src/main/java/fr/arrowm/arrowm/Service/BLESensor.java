package fr.arrowm.arrowm.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import fr.arrowm.arrowm.Activities.HomeAct;
import fr.arrowm.arrowm.Business.Constants;
import fr.arrowm.arrowm.R;

public class BLESensor extends Service {

    public int mConnectionState = Constants.BLE.STATE_DISCONNECTED;
    private final static String TAG = BLESensor.class.getSimpleName();
    BluetoothGatt mBluetoothGatt;
    private static BluetoothGattCharacteristic mSCharacteristic, mModelNumberCharacteristic, mSerialPortCharacteristic, mCommandCharacteristic;
    public String mBluetoothDeviceAddress = "";
    private boolean stop = false;
    //Show that Characteristic is writing or not.
    private boolean mIsWritingCharacteristic = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private SharedPreferences sharedpreferences;
    private RingBuffer<BluetoothGattCharacteristicHelper> mCharacteristicRingBuffer = new RingBuffer<BluetoothGattCharacteristicHelper>(8);
    private PendingIntent pendingIntent;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "New State" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "New State : Connected");
                mConnectionState = Constants.BLE.STATE_CONNECTED;
                sendState(Constants.BLE.STATE_CONNECTED,"");

                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                if (mBluetoothGatt.discoverServices()) {
                    Log.i(TAG, "Attempting to start service discovery:");

                } else {
                    Log.i(TAG, "Attempting to start service discovery:not success");

                }


            //} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            } else {
                Log.i(TAG, "New State : Disconnected");
                mConnectionState = Constants.BLE.STATE_DISCONNECTED;
                if (!stop) {
                    Log.e(TAG, "Disconnected from GATT server.");
                    sendState(Constants.BLE.STATE_CONNECTING,"error_reconnect_BLE");
                    //disconnect();
                    //close();
                    /*Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connect(mBluetoothDeviceAddress);
                        }
                    }, 5000);*/
                }

            }
        }

        private void getGattServices(List<BluetoothGattService> gattServices) {
            if (gattServices == null) return;
            String uuid = null;
            mModelNumberCharacteristic = null;
            mSerialPortCharacteristic = null;
            mCommandCharacteristic = null;
            mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

            // Loops through available GATT Services.
            for (BluetoothGattService gattService : gattServices) {
                uuid = gattService.getUuid().toString();
                Log.i(TAG, "Gatt service found=" + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    uuid = gattCharacteristic.getUuid().toString();
                    if (uuid.equals(Constants.BLE.ModelNumberStringUUID)) {
                        mModelNumberCharacteristic = gattCharacteristic;
                        Log.i(TAG, "mModelNumberCharacteristic  " + mModelNumberCharacteristic.getUuid().toString());
                    } else if (uuid.equals(Constants.BLE.SerialPortUUID)) {
                        mSerialPortCharacteristic = gattCharacteristic;
                        Log.i(TAG, "mSerialPortCharacteristic  " + mSerialPortCharacteristic.getUuid().toString());
                    } else if (uuid.equals(Constants.BLE.CommandUUID)) {
                        mCommandCharacteristic = gattCharacteristic;
                        Log.i(TAG, "mSerialPortCharacteristic  " + mSerialPortCharacteristic.getUuid().toString());
                    }
                }
                mGattCharacteristics.add(charas);
            }

            if (mModelNumberCharacteristic == null || mSerialPortCharacteristic == null || mCommandCharacteristic == null) {
                sendState(Constants.BLE.STATE_DISCONNECTED,"error_not_found_BLE");
            } else {
                mSCharacteristic = mModelNumberCharacteristic;
                setCharacteristicNotification(mSCharacteristic, true);
                readCharacteristic(mSCharacteristic);
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                getGattServices(getSupportedGattServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//this block should be synchronized to prevent the function overloading
            synchronized (this) {
                //CharacteristicWrite success
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "onCharacteristicWrite success:" + new String(characteristic.getValue()));
                    if (mCharacteristicRingBuffer.isEmpty()) {
                        mIsWritingCharacteristic = false;
                    } else {
                        BluetoothGattCharacteristicHelper bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next();
                        if (bluetoothGattCharacteristicHelper.mCharacteristicValue.length() > Constants.BLE.MAX_CHARACTERISTIC_LENGTH) {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(0, Constants.BLE.MAX_CHARACTERISTIC_LENGTH).getBytes("ISO-8859-1"));

                            } catch (UnsupportedEncodingException e) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw new IllegalStateException(e);
                            }


                            if (mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success");
                            } else {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure");
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(Constants.BLE.MAX_CHARACTERISTIC_LENGTH);
                        } else {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.getBytes("ISO-8859-1"));
                            } catch (UnsupportedEncodingException e) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw new IllegalStateException(e);
                            }

                            if (mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success");
                            } else {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure");
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = "";

//	            			System.out.print("before pop:");
//	            			System.out.println(mCharacteristicRingBuffer.size());
                            mCharacteristicRingBuffer.pop();
//	            			System.out.print("after pop:");
//	            			System.out.println(mCharacteristicRingBuffer.size());
                        }
                    }
                }
                //WRITE a NEW CHARACTERISTIC
                else if (status == Constants.BLE.WRITE_NEW_CHARACTERISTIC) {
                    if ((!mCharacteristicRingBuffer.isEmpty()) && mIsWritingCharacteristic == false) {
                        BluetoothGattCharacteristicHelper bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next();
                        if (bluetoothGattCharacteristicHelper.mCharacteristicValue.length() > Constants.BLE.MAX_CHARACTERISTIC_LENGTH) {

                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(0, Constants.BLE.MAX_CHARACTERISTIC_LENGTH).getBytes("ISO-8859-1"));
                            } catch (UnsupportedEncodingException e) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw new IllegalStateException(e);
                            }

                            if (mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success");
                            } else {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure");
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(Constants.BLE.MAX_CHARACTERISTIC_LENGTH);
                        } else {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.getBytes("ISO-8859-1"));
                            } catch (UnsupportedEncodingException e) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw new IllegalStateException(e);
                            }


                            if (mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success");
//	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[0]);
//	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[1]);
//	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[2]);
//	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[3]);
//	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[4]);
//	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[5]);

                            } else {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure");
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = "";

//		            			System.out.print("before pop:");
//		            			System.out.println(mCharacteristicRingBuffer.size());
                            mCharacteristicRingBuffer.pop();
//		            			System.out.print("after pop:");
//		            			System.out.println(mCharacteristicRingBuffer.size());
                        }
                    }

                    mIsWritingCharacteristic = true;

                    //clear the buffer to prevent the lock of the mIsWritingCharacteristic
                    if (mCharacteristicRingBuffer.isFull()) {
                        mCharacteristicRingBuffer.clear();
                        mIsWritingCharacteristic = false;
                    }
                } else
                //CharacteristicWrite fail
                {
                    mCharacteristicRingBuffer.clear();
                    Log.i(TAG, "onCharacteristicWrite fail:" + new String(characteristic.getValue()));
                    Log.i(TAG, status + "");
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicRead  " + characteristic.getUuid().toString());
                broadcastCount(characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor characteristic,
                                      int status) {
            Log.i(TAG, "onDescriptorWrite  " + characteristic.getUuid().toString() + " " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged  " + new String(characteristic.getValue()));
            broadcastCount(characteristic);
        }
    };
    public BLESensor() {
    }

    @Override
    public void onCreate() {
        sharedpreferences = getSharedPreferences(Constants.DECL.PREFERENCES, Context.MODE_PRIVATE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel nc = new NotificationChannel(Constants.BLE.ARROW_CHANNEL,Constants.BLE.ARROW_CHANNEL, NotificationManager.IMPORTANCE_MIN);
        notificationManager.createNotificationChannel(nc);

        Intent notificationIntent = new Intent(this, HomeAct.class);
        notificationIntent.setAction(Constants.BLE.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received Start Foreground Intent ");
        if (intent.getAction().equals(Constants.BLE.STARTFOREGROUND_ACTION)) {

            Notification notification  = new NotificationCompat.Builder(this, Constants.BLE.ARROW_CHANNEL)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getResources().getString(R.string.connected))
                    .setSmallIcon(R.drawable.ic_bluetooth_notif)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOngoing(true).build();
            startForeground(Constants.BLE.FOREGROUND_SERVICE, notification);

            if (initialize()) {
                connect(mBluetoothDeviceAddress);
            }
        } else if (intent.getAction().equals(Constants.BLE.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    public boolean initialize() {
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.DECL.PREFERENCES, Context.MODE_PRIVATE);
        mBluetoothDeviceAddress = sharedpreferences.getString(Constants.DECL.SENSOR_NAME, "");

        if (mBluetoothDeviceAddress.equals("")) {
            Log.e(TAG, "Address not set in Sensor act");
            sendState(0,"Address not set in Sensor act");
            return false;
        } else {
            // For API level 18 and above, get a reference to BluetoothAdapter through
            // BluetoothManager.
            Log.i(TAG, "BluetoothLeService initialize" + mBluetoothManager);
            if (mBluetoothManager == null) {
                mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (mBluetoothManager == null) {
                    sendState(Constants.BLE.STATE_DISCONNECTED,"error_init_BLE");
                    return false;
                }
            }

            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                sendState(Constants.BLE.STATE_DISCONNECTED,"error_init_BLE");
                return false;
            }

            return true;
        }
    }

    public boolean connect(final String address) {
        Log.i(TAG, "BluetoothLeService connect" + address + mBluetoothGatt);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            sendState(Constants.BLE.STATE_DISCONNECTED,"error_init_BLE");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            sendState(Constants.BLE.STATE_DISCONNECTED,"error_not_found_BLE");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        Log.i(TAG, "device.connectGatt connect");
        synchronized (this) {
            //mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        }
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = Constants.BLE.STATE_CONNECTING;
        return true;
    }

    private void broadcastCount(final BluetoothGattCharacteristic characteristic) {
        ArrayList<String> msg;
        Log.i(TAG, "BluetoothLeService broadcastUpdate");
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            if (mSCharacteristic == mModelNumberCharacteristic) {
                if (new String(data).toUpperCase().startsWith("DF BLUNO")) {
                    setCharacteristicNotification(mSCharacteristic, false);
                    mSCharacteristic = mCommandCharacteristic;
                    mSCharacteristic.setValue(Constants.BLE.mPassword);
                    writeCharacteristic(mSCharacteristic);
                    mSCharacteristic.setValue(Constants.BLE.mBaudrateBuffer);
                    writeCharacteristic(mSCharacteristic);
                    mSCharacteristic = mSerialPortCharacteristic;
                    setCharacteristicNotification(mSCharacteristic, true);
                    mConnectionState = Constants.BLE.STATE_CONNECTED;
                    sendState(Constants.BLE.STATE_CONNECTED,"");
                    //

                } else {
                    mConnectionState = Constants.BLE.STATE_CONNECTING;
                    sendState(Constants.BLE.STATE_DISCONNECTED,"");
                }
            } else if (mSCharacteristic == mSerialPortCharacteristic) {
                sendCount(new String(data));
            }
        }
//        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void sendCount(String data) {
        Intent intent = new Intent(Constants.DECL.SENSOR);
        intent.putExtra(Constants.DECL.SENSOR_DATA, Constants.BLE.STATE_CONNECTED + ";" + data + "; ;");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendState(Integer s, String msg) {
        String sent = new String();
        if(msg.equals("")){
            sent = s + ";" + -1 + ";" + -1 + ";" + -1.0F + "; ;";
        }
        else{
            sent = s + ";" + -1 + ";" + -1 + ";" + -1.0F + ";" + msg + ";";
        }
        Intent intent = new Intent(Constants.DECL.SENSOR);
        intent.putExtra(Constants.DECL.SENSOR_DATA, sent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sendState(Constants.BLE.STATE_DISCONNECTED,"");
        stop = true;
        disconnect();
        close();
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        System.out.println("BluetoothLeService disconnect");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        System.out.println("BluetoothLeService close");
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Write information to the device on a given {@code BluetoothGattCharacteristic}. The content string and characteristic is
     * only pushed into a ring buffer. All the transmission is based on the {@code onCharacteristicWrite} call back function,
     * which is called directly in this function
     *
     * @param characteristic The characteristic to write to.
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        //The character size of TI CC2540 is limited to 17 bytes, otherwise characteristic can not be sent properly,
        //so String should be cut to comply this restriction. And something should be done here:
        String writeCharacteristicString;
        try {
            writeCharacteristicString = new String(characteristic.getValue(), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            // this should never happen because "US-ASCII" is hard-coded.
            throw new IllegalStateException(e);
        }
        Log.i(TAG, "allwriteCharacteristicString:" + writeCharacteristicString);

        //As the communication is asynchronous content string and characteristic should be pushed into an ring buffer for further transmission
        mCharacteristicRingBuffer.push(new BluetoothGattCharacteristicHelper(characteristic, writeCharacteristicString));
        Log.i(TAG, "mCharacteristicRingBufferlength:" + mCharacteristicRingBuffer.size());


        //The progress of onCharacteristicWrite and writeCharacteristic is almost the same. So callback function is called directly here
        //for details see the onCharacteristicWrite function
        mGattCallback.onCharacteristicWrite(mBluetoothGatt, characteristic, Constants.BLE.WRITE_NEW_CHARACTERISTIC);

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        //BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getUuid());
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //mBluetoothGatt.writeDescriptor(descriptor);

        // This is specific to Heart Rate Measurement.
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
//        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    //class to store the Characteristic and content string push into the ring buffer.
    private class BluetoothGattCharacteristicHelper {
        BluetoothGattCharacteristic mCharacteristic;
        String mCharacteristicValue;

        BluetoothGattCharacteristicHelper(BluetoothGattCharacteristic characteristic, String characteristicValue) {
            mCharacteristic = characteristic;
            mCharacteristicValue = characteristicValue;
        }
    }

}
