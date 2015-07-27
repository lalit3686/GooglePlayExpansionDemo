package com.app.abc.xyz;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.APKExpansionPolicy;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;

public class MainActivity extends Activity{

	private ProgressDialog mProgressDialog;
	private SharedPreferences mPreferences;
    private String mainFilePath;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mainFilePath = ExpansionUtility.getMainFilePath(this);
		
		mPreferences = getSharedPreferences("PREF_FILE_SIZE", Context.MODE_PRIVATE);
		
		int previousVersionCode = mPreferences.getInt("APP_VERSION_CODE", -1);
		
		if(previousVersionCode < ExpansionUtility.CURRENT_VERSION_CODE){
			
		}
	
		if (!ExpansionUtility.expansionFilesDelivered(this)) {
			requestExpansionFile();
		}
	}
    
    private void requestExpansionFile() {

		showProgressDialog();
		
		String dirPath = ExpansionUtility.getSaveFilePath(this);

		try {
			File file = new File(dirPath);
			if(!file.exists()){
				file.mkdirs();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

		final APKExpansionPolicy aep = new APKExpansionPolicy(this, new AESObfuscator(ExpansionUtility.getSalt(), getPackageName(), deviceId));

		// reset our policy back to the start of the world to force a
		// re-check
		aep.resetPolicy();

		// let's try and get the OBB file from LVL first
		// Construct the LicenseChecker with a Policy.
		final LicenseChecker checker = new LicenseChecker(this, aep,
				ExpansionUtility.getBase64PublicKey() // Your public licensing key.
				);

		checker.checkAccess(new LicenseCheckerCallback() {

			@Override
			public void dontAllow(int reason) {
				Log.e("dontAllow", "dontAllow");
				dismissProgressDialog();
				switch (reason) {
				case Policy.NOT_LICENSED:
					Toast.makeText(getApplicationContext(), "NOT_LICENSED", Toast.LENGTH_LONG).show();
					break;
				case Policy.RETRY:
					Toast.makeText(getApplicationContext(), "RETRY", Toast.LENGTH_LONG).show();
					break;
				default:
					Toast.makeText(getApplicationContext(), "dontAllow", Toast.LENGTH_LONG).show();
					break;
				}
			}

			@Override
			public void applicationError(int errorCode) {
				Toast.makeText(getApplicationContext(), "applicationError", Toast.LENGTH_LONG).show();
				Log.e("applicationError", "applicationError");
				dismissProgressDialog();
			}

			@Override
			public void allow(int reason) {

				int count = aep.getExpansionURLCount();
				if (count != 0) {
					for (int i = 0; i < count; i++) {
						String currentFileName = aep.getExpansionFileName(i);
						if(currentFileName != null){
							long fileSize = aep.getExpansionFileSize(i);
							String expansionFileUrl = aep.getExpansionURL(i);
							Log.e("count*fileSize*url", "count- "+count+", fileSize- "+fileSize+", url- "+expansionFileUrl);
							new DownloadExpansionFileAsyncTask(expansionFileUrl).execute();
						}
					}
				}
			}
		});
	}
    
    private void showProgressDialog() {
		if(mProgressDialog == null){
			mProgressDialog = new ProgressDialog(MainActivity.this);
			mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMessage("downloading...");
			mProgressDialog.setMax(100);
			mProgressDialog.show();
		}
	}
    
    private void dismissProgressDialog() {
		if(mProgressDialog != null && mProgressDialog.isShowing()){
			mProgressDialog.dismiss();
		}
	}
    
    public class DownloadExpansionFileAsyncTask extends AsyncTask<Integer, Integer, Boolean>
	{	
		String expansionFileUrl;

		public DownloadExpansionFileAsyncTask(String expansionFileUrl) {
			this.expansionFileUrl = expansionFileUrl;
		}
		@Override
		protected void onPreExecute() {
			showProgressDialog();
		}
		
		public void setProgress(int downloaded) {
			publishProgress(downloaded);
		}

		@Override
		protected Boolean doInBackground(Integer... params) {
			/**
			 * If expansion URL is NULL then just unzip the DB file, if file is already unzipped return true 
			 */
			if(expansionFileUrl == null){
				mProgressDialog.setMessage("downloading...");
				return ExpansionUtility.copyUnzippedFile(mPreferences, getApplicationContext(), mainFilePath, this);
			}
			else if(ExpansionUtility.downloadExpansionFile(this, expansionFileUrl, mainFilePath)){
				/**
				 * set progress to 0 & max to 100 when downloading of file is completed and unzipping starts 
				 */
				setProgress(-1);
				return ExpansionUtility.copyUnzippedFile(mPreferences, getApplicationContext(), mainFilePath, this);
			}					
			return false;
		}

		/**
		 *  If value is -1 then set progress to 0 and max to 100 for unzipping
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			if(values[0] == -1){
				mProgressDialog.setMax(100);
				mProgressDialog.setProgress(0);
				mProgressDialog.setMessage("unzipping...");
			}
			else{
				mProgressDialog.setProgress(values[0]);
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			dismissProgressDialog();
			if(!result){
				Toast.makeText(getApplicationContext(), "some problem occurred", Toast.LENGTH_LONG).show();
				Log.e("DownloadExpansionFileAsyncTask", "some problem occurred while downloading database.");
			}
			else{
				/**
				 * save the version code
				 */
				Editor editor = mPreferences.edit();
				editor.putInt("APP_VERSION_CODE", ExpansionUtility.CURRENT_VERSION_CODE);
				editor.commit();
			}
		}
	}	
}
