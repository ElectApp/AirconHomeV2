package com.apyeng.airconhomev2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;



public class FileHelder {


    public FileHelder(){

    }

    //Path EX: /raw/storage/emulated/0/DCIM/Camera/IMG_20190525_190134.jpg
    //File name is IMG_20190525_190134.jpg
    public static String getFileName(String fullPath){
        String detail[] = fullPath.split("/");
        int len = detail.length;
        if (len>0){
            int end = len-1;
            return detail[end];
        }
        return fullPath;
    }

    //Thank: https://www.dev2qa.com/how-to-get-real-file-path-from-android-uri/
    public String getUriRealPathAboveKitkat(Context ctx, Uri uri) {
        String ret = "";

        if(ctx != null && uri != null) {

            if(isContentUri(uri)) {
                if(isGooglePhotoDoc(uri.getAuthority()))
                {
                    ret = uri.getLastPathSegment();
                }else {
                    ret = getImageRealPath(ctx.getContentResolver(), uri, null);
                }
            }else if(isFileUri(uri)) {
                ret = uri.getPath();
            }else if(isDocumentUri(ctx, uri)){

                // Get uri related document id.
                String documentId = DocumentsContract.getDocumentId(uri);

                // Get uri authority.
                String uriAuthority = uri.getAuthority();

                if(isMediaDoc(uriAuthority)) {
                    String idArr[] = documentId.split(":");
                    if(idArr.length == 2)
                    {
                        // First item is document type.
                        String docType = idArr[0];

                        // Second item is document real id.
                        String realDocId = idArr[1];

                        // Get content uri by document type.
                        Uri mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        if("image".equals(docType))
                        {
                            mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        }else if("video".equals(docType))
                        {
                            mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        }else if("audio".equals(docType))
                        {
                            mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }

                        // Get where clause with real document id.
                        String whereClause = MediaStore.Images.Media._ID + " = " + realDocId;

                        ret = getImageRealPath(ctx.getContentResolver(), mediaContentUri, whereClause);
                    }

                }else if(isDownloadDoc(uriAuthority)) {
                    // Build download uri.
                    Uri downloadUri = Uri.parse("content://downloads/public_downloads");

                    // Append download document id at uri end.
                    Uri downloadUriAppendId = ContentUris.withAppendedId(downloadUri, Long.valueOf(documentId));

                    ret = getImageRealPath(ctx.getContentResolver(), downloadUriAppendId, null);

                }else if(isExternalStoreDoc(uriAuthority)) {
                    String idArr[] = documentId.split(":");
                    if(idArr.length == 2) {
                        String type = idArr[0];
                        String realDocId = idArr[1];

                        if("primary".equalsIgnoreCase(type)) {
                            ret = Environment.getExternalStorageDirectory() + "/" + realDocId;
                        }
                    }
                }
            }
        }

        return ret;
    }

    /* Check whether this uri represent a document or not. */
    private boolean isDocumentUri(Context ctx, Uri uri) {
        boolean ret = false;
        if(ctx != null && uri != null) {
            ret = DocumentsContract.isDocumentUri(ctx, uri);
        }
        return ret;
    }

    /* Check whether this uri is a content uri or not.
     *  content uri like content://media/external/images/media/1302716
     *  */
    private boolean isContentUri(Uri uri) {
        boolean ret = false;
        if(uri != null) {
            String uriSchema = uri.getScheme();
            if("content".equalsIgnoreCase(uriSchema)) {
                ret = true;
            }
        }
        return ret;
    }

    /* Check whether this uri is a file uri or not.
     *  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
     * */
    private boolean isFileUri(Uri uri) {
        boolean ret = false;
        if(uri != null) {
            String uriSchema = uri.getScheme();
            if("file".equalsIgnoreCase(uriSchema)) {
                ret = true;
            }
        }
        return ret;
    }


    /* Check whether this document is provided by ExternalStorageProvider. */
    private boolean isExternalStoreDoc(String uriAuthority) {

        return "com.android.externalstorage.documents".equals(uriAuthority);
    }

    /* Check whether this document is provided by DownloadsProvider. */
    private boolean isDownloadDoc(String uriAuthority) {

        return "com.android.providers.downloads.documents".equals(uriAuthority);
    }

    /* Check whether this document is provided by MediaProvider. */
    private boolean isMediaDoc(String uriAuthority) {

        return "com.android.providers.media.documents".equals(uriAuthority);
    }

    /* Check whether this document is provided by google photos. */
    private boolean isGooglePhotoDoc(String uriAuthority) {

        return "com.google.android.apps.photos.content".equals(uriAuthority);
    }

    /* Return uri represented document file real local path.*/
    private String getImageRealPath(ContentResolver contentResolver, Uri uri, String whereClause) {
        String ret = "";

        // Query the uri with condition.
        Cursor cursor = contentResolver.query(uri, null, whereClause, null, null);

        if(cursor!=null) {

            boolean moveToFirst = cursor.moveToFirst();
            if(moveToFirst) {

                // Get columns name by uri type.
                String columnName = MediaStore.Images.Media.DATA;

                if( uri==MediaStore.Images.Media.EXTERNAL_CONTENT_URI ) {
                    columnName = MediaStore.Images.Media.DATA;

                }else if( uri==MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ) {
                    columnName = MediaStore.Audio.Media.DATA;

                }else if( uri==MediaStore.Video.Media.EXTERNAL_CONTENT_URI ) {
                    columnName = MediaStore.Video.Media.DATA;

                }

                // Get column index.
                int imageColumnIndex = cursor.getColumnIndex(columnName);

                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex);
            }
        }

        return ret;
    }



}
