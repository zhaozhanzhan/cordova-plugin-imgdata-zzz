package com.zzz;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.widget.Toast;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.google.gson.*;

import com.mirrortech.szwzjujiaapp.R;
import com.qr.print.*;

import id.zelory.compressor.Compressor;

/**
 * This class echoes a string called from JavaScript.
 */
public class ImgData extends CordovaPlugin {
    private static final String TAG = "ImgData";
    Activity activity;
    // execute是必须重写的方法，会有三个构造方法，按需重写自己需要的，
    // execute方法中的action参数是和Toast.js关联使用的，
    // args是js返回的参数，callbackContext是对js的回调
    private CallbackContext readInfoCallBack;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getImgData")) {// 启动扫描，需要手动调用终止扫描方法

            // 返回的回调
            readInfoCallBack = callbackContext;

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            this.cordova.startActivityForResult(this, intent, 1001);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageData) {
        // TODO do something with the bitmap
        super.onActivityResult(requestCode, resultCode, imageData);
        switch (requestCode) {
        case 1001:
            if (imageData != null) {
                String realPath = RealPathFromUriUtils.getRealPathFromUri(activity, imageData.getData());
                Log.d(TAG, "onActivityResult:相册 " + realPath);
                dealToImage(realPath);
            }
            break;
        }
    }

    /**
     * 进行图片处理
     */
    private void dealToImage(final String realPath) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    JSONObject imageJson = (JSONObject) msg.obj;
                    Log.i("ReadPhotoInfo", imageJson.toString());
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, imageJson);
                    pluginResult.setKeepCallback(true);
                    readInfoCallBack.sendPluginResult(pluginResult);
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 进行图片压缩
                String imageBase64 = initCompressorIOFile(realPath);
                JSONObject imageJson = new JSONObject();
                try {
                    imageJson = readPhotoInfo(realPath);
                    try {
                        imageJson.put("image", imageBase64);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "onActivityResult  图片信息： " + imageJson.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // TODO Auto-generated method stub
                Message message = new Message();
                message.what = 1;
                message.obj = imageJson;
                handler.sendMessage(message);
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    private JSONObject readPhotoInfo(String img_path) throws IOException {
        ExifInterface exifInterface = new ExifInterface(img_path);

        String TAG_APERTURE = exifInterface.getAttribute(ExifInterface.TAG_APERTURE);
        String TAG_DATETIME = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        String TAG_EXPOSURE_TIME = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        String TAG_FLASH = exifInterface.getAttribute(ExifInterface.TAG_FLASH);
        String TAG_FOCAL_LENGTH = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
        String TAG_IMAGE_LENGTH = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
        String TAG_IMAGE_WIDTH = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        String TAG_ISO = exifInterface.getAttribute(ExifInterface.TAG_ISO);
        String TAG_MAKE = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        String TAG_MODEL = exifInterface.getAttribute(ExifInterface.TAG_MODEL);
        String TAG_ORIENTATION = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
        String TAG_WHITE_BALANCE = exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
        String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String latitude_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String longitude_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("aperture", TAG_APERTURE); // 光圈值
            jsonObject.put("datetime", TAG_DATETIME); // 拍摄时间
            jsonObject.put("exposure_time", TAG_EXPOSURE_TIME); // 曝光时间
            jsonObject.put("flash", TAG_FLASH); // 闪光灯
            jsonObject.put("focal_length", TAG_FOCAL_LENGTH); // 焦距
            jsonObject.put("image_length", TAG_IMAGE_LENGTH); // 图片高度
            jsonObject.put("image_width", TAG_IMAGE_WIDTH); // 图片宽度
            jsonObject.put("iso", TAG_ISO); // iso
            jsonObject.put("make", TAG_MAKE); // 设备品牌
            jsonObject.put("model", TAG_MODEL); // 设备型号
            jsonObject.put("orientation", TAG_ORIENTATION); // 旋转角度
            jsonObject.put("white_balance", TAG_WHITE_BALANCE); // 白平衡
            jsonObject.put("latitude", latitude);
            jsonObject.put("latitude_ref", latitude_ref);
            jsonObject.put("longitude", longitude);
            jsonObject.put("longitude_ref", longitude_ref);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    static class RealPathFromUriUtils {
        /**
         * 根据Uri获取图片的绝对路径
         *
         * @param context 上下文对象
         * @param uri     图片的Uri
         * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
         */
        public static String getRealPathFromUri(Context context, Uri uri) {
            int sdkVersion = Build.VERSION.SDK_INT;
            if (sdkVersion >= 19) { // api >= 19
                return getRealPathFromUriAboveApi19(context, uri);
            } else { // api < 19
                return getRealPathFromUriBelowAPI19(context, uri);
            }
        }

        /**
         * 适配api19以下(不包括api19),根据uri获取图片的绝对路径
         *
         * @param context 上下文对象
         * @param uri     图片的Uri
         * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
         */
        private static String getRealPathFromUriBelowAPI19(Context context, Uri uri) {
            return getDataColumn(context, uri, null, null);
        }

        /**
         * 适配api19及以上,根据uri获取图片的绝对路径
         *
         * @param context 上下文对象
         * @param uri     图片的Uri
         * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
         */
        @SuppressLint("NewApi")
        private static String getRealPathFromUriAboveApi19(Context context, Uri uri) {
            String filePath = null;
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // 如果是document类型的 uri, 则通过document id来进行处理
                String documentId = DocumentsContract.getDocumentId(uri);
                if (isMediaDocument(uri)) { // MediaProvider
                    // 使用':'分割
                    String id = documentId.split(":")[1];

                    String selection = MediaStore.Images.Media._ID + "=?";
                    String[] selectionArgs = { id };
                    filePath = getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection,
                            selectionArgs);
                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(documentId));
                    filePath = getDataColumn(context, contentUri, null, null);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                // 如果是 content 类型的 Uri
                filePath = getDataColumn(context, uri, null, null);
            } else if ("file".equals(uri.getScheme())) {
                // 如果是 file 类型的 Uri,直接获取图片对应的路径
                filePath = uri.getPath();
            }
            return filePath;
        }

        /**
         * 获取数据库表中的 _data 列，即返回Uri对应的文件路径
         *
         * @return
         */
        private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
            String path = null;

            String[] projection = new String[] { MediaStore.Images.Media.DATA };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                    path = cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return path;
        }

        /**
         * @param uri the Uri to check
         * @return Whether the Uri authority is MediaProvider
         */
        private static boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri the Uri to check
         * @return Whether the Uri authority is DownloadsProvider
         */
        private static boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }
    }

    /**
     * 使用Compressor IO模式压缩返回File
     */
    private String initCompressorIOFile(String path) {
        String imageBase64 = "";
        try {
            File file = new Compressor(activity).setMaxWidth(640).setMaxHeight(480).setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.WEBP)
                    .setDestinationDirectoryPath(Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath())
                    .compressToFile(new File(path));
            imageBase64 = imageToBase64(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageBase64;
    }

    /**
     * 将图片转换成Base64编码的字符串
     */
    public static String imageToBase64(File file) {

        InputStream is = null;
        byte[] data = null;
        String result = null;
        try {
            is = new FileInputStream(file);
            // 创建一个字符流大小的数组。
            data = new byte[is.available()];
            // 写入数组
            is.read(data);
            // 用默认的编码格式进行编码
            result = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

}
