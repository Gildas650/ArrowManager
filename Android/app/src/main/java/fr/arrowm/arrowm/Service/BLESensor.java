package fr.arrowm.arrowm.Service;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BLESensor extends Service {
    public static final String PREFERENCES = "ArrowPrefs";
    public static final String SENSOR_NAME = "sensorName";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_DATA = "sensorData";
    public static final String DELIMITERS = ";";
    public static final String SerialPortUUID = "0000dfb1-0000-1000-8000-00805f9b34fb";
    public static final String CommandUUID = "0000dfb2-0000-1000-8000-00805f9b34fb";
    public static final String ModelNumberStringUUID = "00002a24-0000-1000-8000-00805f9b34fb";
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
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    //To tell the onCharacteristicWrite call back function that this is a new characteristic,
    //not the Write Characteristic to the device successfully.
    private static final int WRITE_NEW_CHARACTERISTIC = -1;
    //define the limited length of the characteristic.
    private static final int MAX_CHARACTERISTIC_LENGTH = 17;
    private final static String TAG = BLESensor.class.getSimpleName();
    private static BluetoothGattCharacteristic mSCharacteristic, mModelNumberCharacteristic, mSerialPortCharacteristic, mCommandCharacteristic;
    public String mBluetoothDeviceAddress = "";
    public int mConnectionState = STATE_DISCONNECTED;
    BluetoothGatt mBluetoothGatt;
    private int mBaudrate = 115200;
    private String mPassword = "AT+PASSWOR=DFRobot\r\n";
    private String mBaudrateBuffer = "AT+CURRUART=" + mBaudrate + "\r\n";
    private boolean stop = false;
    //Show that Characteristic is writing or not.
    private boolean mIsWritingCharacteristic = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private SharedPreferences sharedpreferences;
    private RingBuffer<BluetoothGattCharacteristicHelper> mCharacteristicRingBuffer = new RingBuffer<BluetoothGattCharacteristicHelper>(8);
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "New State" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "New State : Connected");
                mConnectionState = STATE_CONNECTED;
                sendState(STATE_CONNECTED,"");
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
                mConnectionState = STATE_DISCONNECTED;
                if (!stop) {
                    Log.e(TAG, "Disconnected from GATT server.");
                    sendState(STATE_CONNECTING,"error_reconnect_BLE");
                    disconnect();
                    close();
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connect(mBluetoothDeviceAddress);
                        }
                    }, 5000);
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
                    if (uuid.equals(ModelNumberStringUUID)) {
                        mModelNumberCharacteristic = gattCharacteristic;
                        Log.i(TAG, "mModelNumberCharacteristic  " + mModelNumberCharacteristic.getUuid().toString());
                    } else if (uuid.equals(SerialPortUUID)) {
                        mSerialPortCharacteristic = gattCharacteristic;
                        Log.i(TAG, "mSerialPortCharacteristic  " + mSerialPortCharacteristic.getUuid().toString());
                    } else if (uuid.equals(CommandUUID)) {
                        mCommandCharacteristic = gattCharacteristic;
                        Log.i(TAG, "mSerialPortCharacteristic  " + mSerialPortCharacteristic.getUuid().toString());
                    }
                }
                mGattCharacteristics.add(charas);
            }

            if (mModelNumberCharacteristic == null || mSerialPortCharacteristic == null || mCommandCharacteristic == null) {
                sendState(STATE_DISCONNECTED,"error_not_found_BLE");
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
                        if (bluetoothGattCharacteristicHelper.mCharacteristicValue.length() > MAX_CHARACTERISTIC_LENGTH) {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(0, MAX_CHARACTERISTIC_LENGTH).getBytes("ISO-8859-1"));

                            } catch (UnsupportedEncodingException e) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw new IllegalStateException(e);
                            }


                            if (mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success");
                            } else {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure");
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(MAX_CHARACTERISTIC_LENGTH);
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
                else if (status == WRITE_NEW_CHARACTERISTIC) {
                    if ((!mCharacteristicRingBuffer.isEmpty()) && mIsWritingCharacteristic == false) {
                        BluetoothGattCharacteristicHelper bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next();
                        if (bluetoothGattCharacteristicHelper.mCharacteristicValue.length() > MAX_CHARACTERISTIC_LENGTH) {

                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(0, MAX_CHARACTERISTIC_LENGTH).getBytes("ISO-8859-1"));
                            } catch (UnsupportedEncodingException e) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw new IllegalStateException(e);
                            }

                            if (mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success");
                            } else {
                                Log.i(TAG, "writeCharacteristic init " + new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure");
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(MAX_CHARACTERISTIC_LENGTH);
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
        sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        if (initialize()) {
            connect(mBluetoothDeviceAddress);
        }

    }

    public boolean initialize() {
        SharedPreferences sharedpreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        mBluetoothDeviceAddress = sharedpreferences.getString(SENSOR_NAME, "");

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
                    sendState(STATE_DISCONNECTED,"error_init_BLE");
                    return false;
                }
            }

            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                sendState(STATE_DISCONNECTED,"error_init_BLE");
                return false;
            }

            return true;
        }
    }

    public boolean connect(final String address) {
        Log.i(TAG, "BluetoothLeService connect" + address + mBluetoothGatt);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            sendState(STATE_DISCONNECTED,"error_init_BLE");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            sendState(STATE_DISCONNECTED,"error_not_found_BLE");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        Log.i(TAG, "device.connectGatt connect");
        synchronized (this) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            //mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        }
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
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
                    mSCharacteristic.setValue(mPassword);
                    writeCharacteristic(mSCharacteristic);
                    mSCharacteristic.setValue(mBaudrateBuffer);
                    writeCharacteristic(mSCharacteristic);
                    mSCharacteristic = mSerialPortCharacteristic;
                    setCharacteristicNotification(mSCharacteristic, true);
                    mConnectionState = STATE_CONNECTED;
                    sendState(STATE_CONNECTED,"");
                    //

                } else {
                    mConnectionState = STATE_CONNECTING;
                    sendState(STATE_DISCONNECTED,"");
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
        Intent intent = new Intent(SENSOR);
        intent.putExtra(SENSOR_DATA, STATE_CONNECTED + ";" + data + "; ;");
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
        Intent intent = new Intent(SENSOR);
        intent.putExtra(SENSOR_DATA, sent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sendState(STATE_DISCONNECTED,"");
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
        mGattCallback.onCharacteristicWrite(mBluetoothGatt, characteristic, WRITE_NEW_CHARACTERISTIC);

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
