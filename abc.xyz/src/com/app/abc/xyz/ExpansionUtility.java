package com.app.abc.xyz;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

import com.app.abc.xyz.MainActivity.DownloadExpansionFileAsyncTask;



public class ExpansionUtility{

	private static final String LOG_TAG = "ExpansionUtility";
	// stuff for LVL -- MODIFY FOR YOUR APPLICATION!
    private static final String BASE64_PUBLIC_KEY = "YOUR PUBLIC KEY";
    
    // used by the preference obfuscater
    private static final byte[] SALT = new byte[] {
            1, 43, -12, -1, 54, 98,
            -100, -12, 43, 2, -8, -4, 9, 5, -106, -108, -33, 45, -1, 84
    };
    
	private static final String EXP_PATH = File.separator + "Android"
            + File.separator + "obb" + File.separator;
	
	// CURRENT VERSION CODE
	public static final int CURRENT_VERSION_CODE = 1;
	// EXPANSION FILE SIZE
	private static final long EXPANSION_FILE_SIZE = 67691035L;
	
	
	public static String getBase64PublicKey() {
		return BASE64_PUBLIC_KEY;
	}
	
	public static byte[] getSalt() {
		return SALT;
	}
	
	/**
     * This is a little helper class that demonstrates simple testing of an
     * Expansion APK file delivered by Market. You may not wish to hard-code
     * things such as file lengths into your executable... and you may wish to
     * turn this code off during application development.
     */
	private static class XAPKFile {
		public final boolean mIsMain;
		public final int mFileVersion;
		public final long mFileSize;
 
		XAPKFile(boolean isMain, int fileVersion, long fileSize) {
			mIsMain = isMain;
			mFileVersion = fileVersion;
			mFileSize = fileSize;
		}
	}
	
	/**
	 * Here is where you place the data that the validator will use to determine
	 * if the file was delivered correctly. This is encoded in the source code
	 * so the application can easily determine whether the file has been
	 * properly delivered without having to talk to the server. If the
	 * application is using LVL for licensing, it may make sense to eliminate
	 * these checks and to just rely on the server.
	 */
	private static final XAPKFile[] xAPKS = {
		new XAPKFile(
			true, // true signifies a main file
			CURRENT_VERSION_CODE, // the version of the APK that the file was uploaded against
			EXPANSION_FILE_SIZE // the length of the file in bytes
		)
	};
	
	 
    /**
     * Go through each of the APK Expansion files defined in the structure above
     * and determine if the files are present and match the required size. Free
     * applications should definitely consider doing this, as this allows the
     * application to be launched for the first time without having a network
     * connection present. Paid applications that use LVL should probably do at
     * least one LVL check that requires the network to be present, so this is
     * not as necessary.
     * 
     * @return true if they are present.
     */
	public static boolean expansionFilesDelivered(Context mContext) {
		for (XAPKFile xf : xAPKS) {
			String fileName = getExpansionAPKFileName(mContext, xf.mIsMain, xf.mFileVersion);
			 Log.e(LOG_TAG, "XAPKFile name : " + fileName);
			if (!doesFileExist(mContext, fileName, xf.mFileSize, false)) {
				Log.e(LOG_TAG, "ExpansionAPKFile doesn't exist or has a wrong size (" + fileName + ").");
				return false;
			}
		}
		return true;
	}
	
	 /**
     * Helper function to ascertain the existence of a file and return
     * true/false appropriately
     * 
     * @param c the app/activity/service context
     * @param fileName the name (sans path) of the file to query
     * @param fileSize the size that the file must match
     * @param deleteFileOnMismatch if the file sizes do not match, delete the
     *            file
     * @return true if it does exist, false otherwise
     */
    private static boolean doesFileExist(Context c, String fileName, long fileSize,
            boolean deleteFileOnMismatch) {
        // the file may have been delivered by Market --- let's make sure
        // it's the size we expect
    	
    	Log.e("doesFileExist", "XAPKFile file length : " + fileSize);
        File fileForNewFile = new File(generateSaveFileName(c, fileName));
        Log.e("fileForNewFile", "file path : " + fileForNewFile.getAbsolutePath());
        if (fileForNewFile.exists()) {
        	Log.e("fileForNewFile.exists()", "XAPKFile file length : " + fileForNewFile.length());
            if (fileForNewFile.length() == fileSize) {
                return true;
            }
            if (deleteFileOnMismatch) {
                // delete the file --- we won't be able to resume
                // because we cannot confirm the integrity of the file
                fileForNewFile.delete();
            }
        }
        return false;
    }
    
    /**
     * Returns the filename (where the file should be saved) from info about a
     * download
     */
    private static String generateSaveFileName(Context c, String fileName) {
        String path = getSaveFilePath(c) + File.separator + fileName;
        Log.e("generateSaveFileName", "XAPKFile name : " + fileName);
        return path;
    }
    
    public static String getSaveFilePath(Context c) {
        File root = Environment.getExternalStorageDirectory();
        String path = root.toString() + EXP_PATH + c.getPackageName();
        return path;
    }
    
    /**
     * Returns the file name (without full path) for an Expansion APK file from
     * the given context.
     * 
     * @param c the context
     * @param mainFile true for main file, false for patch file
     * @param versionCode the version of the file
     * @return String the file name of the expansion file
     */
    public static String getExpansionAPKFileName(Context c, boolean mainFile, int versionCode) {
        return (mainFile ? "main." : "patch.") + versionCode + "." + c.getPackageName() + ".obb";
    }
    
    public static String getMainFilePath(Context c) {
    	PackageInfo pInfo;
		try {
			pInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
			return getSaveFilePath(c) +"/"+ getExpansionAPKFileName(c, true, pInfo.versionCode);
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}
		return null;
	}
    
    public static String getPatchFilePath(Context c) {
    	PackageInfo pInfo;
		try {
			pInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
			return getSaveFilePath(c) +"/"+ getExpansionAPKFileName(c, false, pInfo.versionCode);
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}
		return null;
	}
    
    /***
     * 
     * downloading Expansion file with progress and resuming download from where it stops.
     * 
     */
    public static boolean downloadExpansionFile(DownloadExpansionFileAsyncTask mDownloadExpansionFile, String urlString, String mainFilePath) {

    	long total = 0;
    	long fileLength;
    	
    	try {
    		URL url = new URL(urlString);
    		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    		File file=new File(mainFilePath);
    		FileOutputStream fos;

    		/**
    		 * Resume download from where it stopped
    		 */
    		if(file.exists()){
    			total = (int) file.length();
    			connection.setRequestProperty("Range", "bytes="+total+"-");
    			fos = new FileOutputStream(file, true);
    			fileLength = EXPANSION_FILE_SIZE;
    		}
    		else{
    			connection.setRequestProperty("Range", "bytes=" + total + "-"); 
    			fos = new FileOutputStream(file);
    			fileLength = connection.getContentLength();
    		}


    		InputStream in = new BufferedInputStream(connection.getInputStream());
    		BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
    		byte[] data = new byte[1024];
    		int length = 0;
    		int downloaded = 0;
    		while ((length = in.read(data, 0, 1024)) >= 0) {
    			bout.write(data, 0, length);
    			total += length;
    			downloaded = (int) (total * 100 / fileLength);
    			mDownloadExpansionFile.setProgress(downloaded);
    			Log.e("downloaded", total +" out of "+fileLength+" in % "+downloaded);
    		}
    		bout.close();
    		
    		return true;
    	} 
    	catch (Exception e) {
    		e.printStackTrace();
    	}
		return false;
    }
    
    /**
	 * 
	 * Unzip file 
	 * returns true if file already exists with same size
	 * 
	 */
	public static boolean copyUnzippedFile(SharedPreferences mPreferences, Context mContext, String zipFilePath, DownloadExpansionFileAsyncTask mDownloadExpansionFile) {
		try {
			long dbSize = mPreferences.getLong("GET_EXPANSION_DB_FILE_SIZE", -1);
			
    		File dbFile = new File(Environment.getExternalStorageDirectory().toString()+"/test.db");
	        
	        if(dbFile.exists() && (dbSize == dbFile.length())){
	        	 Log.e(LOG_TAG, "File already exists "+dbFile.length());
	        	 return true;
	        }
	        else{
	        	try {
	        		dbFile.createNewFile();
	        		FileInputStream inputStream = new FileInputStream(zipFilePath);
	        		ZipInputStream mZipInputStream = new ZipInputStream(inputStream);
	        		ZipEntry mZipEntry;
	        		while ((mZipEntry = mZipInputStream.getNextEntry()) != null) { 
	        			
	        			long fileLength = mZipEntry.getSize();
	        			
	        			/**
	        			 * save the size of file
	        			 */
	        			Editor editor = mPreferences.edit();
	        			editor.putLong("GET_EXPANSION_DB_FILE_SIZE", fileLength);
	        			editor.commit();

	        			Log.e("mZipEntry.getName() & mZipEntry.getSize()", mZipEntry.getName()+" - "+mZipEntry.getSize());
	        			
	        			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dbFile));
	        			
	        			byte[] buffer = new byte[8192];
	        			int length;
	        			long total = 0;
	        			int unzipped = 0;
	        			
	        			while ((length = mZipInputStream.read(buffer)) > 0) {
	        				total += length;
	        				unzipped = (int) (total * 100 / fileLength);
	        				mDownloadExpansionFile.setProgress(unzipped);
	        				Log.e("unzipped", total+" out of "+fileLength+" in % "+unzipped);
	        				out.write(buffer, 0, length);
	        			}
	        			out.close();
	        			
	        			return true;
	        		}
	        		mZipInputStream.closeEntry();
	        		mZipInputStream.close();
	        	} catch (IOException e) {
	        		e.printStackTrace();
	        	}
	        }
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
