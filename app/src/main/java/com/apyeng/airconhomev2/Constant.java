package com.apyeng.airconhomev2;

import android.Manifest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constant {

    //URL path on server
    private static final String HOST = "http://apyeng.ddns.net:99/aircon-home-v2/";
    public static final String SIGN_UP_URL = HOST + "sign-up.php";
    public static final String EMAIL_SIGN_IN_URL = HOST + "sign-in-with-email.php";
    public static final String RESEND_EMAIL_VERIFY = HOST + "resend-email-verification.php";
    public static final String ID_SIGN_IN_URL = HOST + "sign-in-with-id.php";
    public static final String ADD_EDIT_GROUP_URL = HOST + "add-edit-group.php";
    public static final String GROUP_DEVICE_LIST = HOST + "group-device.php";
    public static final String DOWNLOAD_URL = HOST + "download.php";
    public static final String DEVICE_LIST = HOST + "device-list.php";
    public static final String JOIN_GROUP_URL = HOST + "join-group.php";
    public static final String UPLOAD_IMG_URL = HOST + "upload-image.php";
    public static final String LEAVE_GROUP_URL = HOST + "leave-group.php";
    public static final String MEMBER_LIST_URL = HOST + "member-list.php";
    public static final String EDIT_USER_DATA_URL = HOST + "edit-user-data.php";
    public static final String UPDATE_TIME_USER_GROUP_URL = HOST + "update-time-user-group.php";
    public static final String DAILY_LOG_DATA = HOST + "daily-log-data.php";
    public static final String DEVICE_LOG_DATA_URL = HOST + "device-log-data.php";
    public static final String MONTHLY_LOG_DATA = HOST + "monthly-log-data.php";
    public static final String DEVICE_FIRMWARE = HOST + "device-firmware.php";
    public static final String SEND_RESET_PASSWORD_EMAIL = HOST + "send-email-reset-password.php";
    public static final String INSERT_UPDATE_ANY_GROUP_TABLE  = HOST + "insert-update-any-group-table.php";

    //Upload path
    private static final String IMAGE_TYPE = "images/";
    public static final String GROUP_IMG_DIR= IMAGE_TYPE+"group";
    public static final String USER_IMG_DIR = IMAGE_TYPE+"user";

    //File type on images folder (server)
    public static final String JPEG = ".jpeg";

    //Scale size
    public static final int IMG_W = 550; //Width size


    //Column name on SQL table and same key on PHP scripts
    public static final String USERNAME = "username", EMAIL = "email", PASSWORD = "password",
        STATUS = "status", USER_ID = "user_id", GROUP_ID = "group_id", VERIFY_CODE = "verification_code",
        NAME = "name", LOCATION = "location", DEVICE_ID = "device_id", PROFILE_IMG = "profile_img",
        ACTUAL_NAME = "actual_name", NICKNAME = "nickname", REGISTERED = "registered", FILENAME = "filename",
            DETAILS = "details", MD5 = "md5";

    //Other key on PHP scripts
    public static final String LANGUAGE = "language", GROUP_ID_LIST = "group_id_list",
            SIGN_OUT = "sign-out", START_POINT = "start_point", TIME = "time", AC_POWER = "ac_power",
            PV_POWER = "pv_power", DATE = "date", START_DATE = "start-date", END_DATE = "end-date",
            PV_WH = "pv_wh", START_MONTH = "start-month", END_MONTH = "end-month",
            SQL_MESSAGE = "sql-message";

    //Result for PHP scripts
    public static final String SUCCESS = "success", CAUSE = "cause", ERROR = "error",
            NO_LIST = "no list", LOG_ERROR = "log_error", SUMMARY_ERROR = "summary_error",
            SUMMARY_DATA = "summary_data", LOG_DATA = "log_data", DATA = "data";

    public static int SIGN_IN_FLAG = 2, SIGN_OUT_FLAG = 3;

    //Permission
    public static final String LOCATION_PERMISSION[] = { Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION };
    public static final int LOCATION_CODE = 111;

    public static final String WRITE_EXTERNAL[] = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
    public static final int WRITE_CODE = 121;

    public static final String CAMERA_PERMISSION[] = { Manifest.permission.CAMERA };
    public static final int CAMERA_CODE = 131;

    //Bundle Key or Intent Key
    public static final String NO_NETWORK = "no-network", NEW_USER = "new-user",
            RESULT_CODE = "result-code", AC_DATA = "ac-data", INDOOR_DATA = "indoor-data",
            HOME_DATA = "home-data", USER_DATA = "user-data", TIME_ON = "time-on", TIME_OFF = "time-off",
            DEVICE_HEAD = "device-head", VERSION = "version", POSITION_ID = "position-id";

    //Unique code for ActivityResult
    public static final int SELECT_PHOTO = 1728;

    //MQTT
    public static final String MQTT_URL = "tcp://apyeng.ddns.net:1883";
    public static final String MQTT_USERNAME = "frecon";
    public static final String MQTT_PASSWORD = "frecon";
    //Key on topic
    public static final String AC = "ac", PV = "pv", STATE = "state", DEVICE_ADDED = "device-added",
            DEVICE_DELETED = "device-deleted", DEVICE_CHANGED = "device-changed",
            GROUP_DELETED = "group-deleted", REQUEST = "request", RESPONSE = "response",
            FIRMWARE_UPDATED = "firmware-updated", FIRMWARE_UPDATING = "firmware-updating";

    public static final String GROUP_KEY[] = { DEVICE_DELETED, DEVICE_CHANGED, DEVICE_ADDED };

    public static final SimpleDateFormat REGISTER_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final DateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.US);

    private static final int NUM_DAY = 30, NUM_WEEK = 4, NUM_MONTH = 12, NUM_YEAR = 5;



}
