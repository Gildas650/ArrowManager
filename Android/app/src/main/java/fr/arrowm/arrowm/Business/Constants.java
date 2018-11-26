package fr.arrowm.arrowm.Business;

public class Constants {
    public interface DECL {
        String IS_BLUETOOTHON = "com.arrowM.MESSAGE2";
        String PREFERENCES = "ArrowPrefs";
        String OBJ_WEEK = "objWeek";
        String OBJ_MONTH = "objMonth";
        String OBJ_SCE = "objSce";
        String SENSOR = "sensor";
        String SENSOR_NAME = "sensorName";
        String SENSOR_DATA = "sensorData";
        String DELIMITERS = ";";
        String SESSION = "com.arrowM.SESSION";
        String BOW_TYPE = "bowType";
        String TOL_TIMING = "tolTiming";
        int WAITING_PERIOD = 10;
    }
    public interface BLE {
        int REQUEST_ENABLE_BT = 1;
        int STATE_DISCONNECTED = 0;
        int STATE_CONNECTING = 1;
        int STATE_CONNECTED = 2;
        float LOWBAT = 3.4F;
        String SerialPortUUID = "0000dfb1-0000-1000-8000-00805f9b34fb";
        String CommandUUID = "0000dfb2-0000-1000-8000-00805f9b34fb";
        String ModelNumberStringUUID = "00002a24-0000-1000-8000-00805f9b34fb";
        int WRITE_NEW_CHARACTERISTIC = -1;
        int MAX_CHARACTERISTIC_LENGTH = 17;
        String STARTFOREGROUND_ACTION ="fr.arrowm.arrowm.action.startforeground";
        String STOPFOREGROUND_ACTION ="fr.arrowm.arrowm.action.stopforeground";
        String MAIN_ACTION = "fr.arrowm.arrowm.action.main";
        String ARROW_CHANNEL ="ARROW_CHANNEL";
        int FOREGROUND_SERVICE = 101;
        int mBaudrate = 115200;
        String mPassword = "AT+PASSWOR=DFRobot\r\n";
        String mBaudrateBuffer = "AT+CURRUART=" + mBaudrate + "\r\n";
    }

    public interface SERVICE {
        String STARTFOREGROUND_ACTION ="fr.arrowm.arrowm.action.startforeground";
        String STOPFOREGROUND_ACTION ="fr.arrowm.arrowm.action.stopforeground";
    }

    public interface TARGET {
        String TRI_SPOT = "FITA Tri-Spot";
        String SPOT = "FITA Mono-Spot";
        String STANDARD = "FITA";
    }

    public interface IMPACT {
        int OFFSETC = 150;
        int POINTSIZE = 15;
    }



}