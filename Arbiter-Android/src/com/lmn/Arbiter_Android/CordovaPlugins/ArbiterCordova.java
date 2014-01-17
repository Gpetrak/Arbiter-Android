package com.lmn.Arbiter_Android.CordovaPlugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.lmn.Arbiter_Android.ArbiterProject;
import com.lmn.Arbiter_Android.ArbiterState;
import com.lmn.Arbiter_Android.OOMWorkaround;
import com.lmn.Arbiter_Android.R;
import com.lmn.Arbiter_Android.Util;
import com.lmn.Arbiter_Android.Activities.MapChangeHelper;
import com.lmn.Arbiter_Android.Activities.TileConfirmation;
import com.lmn.Arbiter_Android.BaseClasses.Feature;
import com.lmn.Arbiter_Android.CordovaPlugins.Helpers.FeatureHelper;
import com.lmn.Arbiter_Android.DatabaseHelpers.FeatureDatabaseHelper;
import com.lmn.Arbiter_Android.DatabaseHelpers.ProjectDatabaseHelper;
import com.lmn.Arbiter_Android.DatabaseHelpers.CommandExecutor.CommandExecutor;
import com.lmn.Arbiter_Android.DatabaseHelpers.TableHelpers.ControlPanelHelper;
import com.lmn.Arbiter_Android.DatabaseHelpers.TableHelpers.FeaturesHelper;
import com.lmn.Arbiter_Android.DatabaseHelpers.TableHelpers.PreferencesHelper;
import com.lmn.Arbiter_Android.Dialog.Dialogs.FeatureDialog.FeatureDialog;
import com.lmn.Arbiter_Android.Dialog.Dialogs.FeatureDialog.MediaSyncHelper;
import com.lmn.Arbiter_Android.Map.Map;
import com.lmn.Arbiter_Android.ProjectStructure.ProjectStructure;

public class ArbiterCordova extends CordovaPlugin{
	private static final String TAG = "ArbiterCordova";
	private final ArbiterProject arbiterProject;
	public static final String mainUrl = "file:///android_asset/www/main.html";
	public static final String aoiUrl = "file:///android_asset/www/aoi.html";
	
	private ProgressDialog mediaUploadProgressDialog;
	private ProgressDialog mediaDownloadProgressDialog;
	
	public ArbiterCordova(){
		super();
		this.arbiterProject = ArbiterProject.getArbiterProject();
		this.mediaUploadProgressDialog = null;
		this.mediaDownloadProgressDialog = null;
	}
	
	public class MediaSyncingTypes {
		public static final int UPLOADING = 0;
		public static final int DOWNLOADING = 1;
	}
	
	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
		
		if("setProjectsAOI".equals(action)){
			
			String aoi = args.getString(0);
			String tileCount = args.getString(1);
			
			setProjectsAOI(aoi, tileCount);
			
			return true;
		}else if("resetWebApp".equals(action)){
			String extent = args.getString(0);
            String zoomLevel = args.getString(1);
            
			resetWebApp(extent, zoomLevel, callbackContext);
			
			return true;
		}else if("confirmTileCount".equals(action)){
			String count = args.getString(0);
			
			confirmTileCount(count);
		}else if("setNewProjectsAOI".equals(action)){
			String aoi = args.getString(0);
			
			setNewProjectsAOI(aoi, callbackContext);
			
			return true;
		}else if("doneCreatingProject".equals(action)){
			
			doneCreatingProject(callbackContext);
			
			return true;
		}else if("errorCreatingProject".equals(action)){
			errorCreatingProject(callbackContext);
			
			return true;
		}else if("errorLoadingFeatures".equals(action)){
			errorLoadingFeatures();
			
			return true;
		}else if("doneAddingLayers".equals(action)){
			doneAddingLayers();
			
			return true;
		}else if("errorAddingLayers".equals(action)){
			String error = args.getString(0);
			
			errorAddingLayers(error);
			
			return true;
		}else if("featureSelected".equals(action)){
			String featureType = args.getString(0);
			String featureId = args.getString(1);
			String layerId = args.getString(2);
			String wktGeometry = args.getString(3);
			String mode = args.getString(4);
			
			featureSelected(featureType, featureId,
					layerId, wktGeometry, mode);
			
			return true;
		}else if("updateTileSyncingStatus".equals(action)){
			String percentComplete = args.getString(0);
			
			updateTileSyncingStatus(percentComplete);
			
			return true;
		}else if("createProjectTileSyncingStatus".equals(action)){
			String percentComplete = args.getString(0);
			
			createProjectTileSyncingStatus(percentComplete);
			
			return true;
		}else if("syncCompleted".equals(action)){
			syncCompleted(callbackContext);
			
			return true;
		}else if("syncFailed".equals(action)){
			syncFailed((args.length() > 0) 
					? args.getString(0) : null, callbackContext);
			
			return true;
		}else if("errorUpdatingAOI".equals(action)){
			errorUpdatingAOI(args.getString(0), callbackContext);
		}else if("addMediaToFeature".equals(action)){
			String key = args.getString(0);
			String media = args.getString(1);
			String newMedia = args.getString(2);
			
			addMediaToFeature(key, media, newMedia);
		}else if("updateMediaUploadingStatus".equals(action)){
			String layer = args.getString(0);
			String finished = args.getString(1);
			String total = args.getString(2);
			String finishedLayers = args.getString(3);
			String totalLayers = args.getString(4);
			
			updateMediaSyncingStatus(layer, finished, total, 
					MediaSyncingTypes.UPLOADING, finishedLayers,
					totalLayers);
		}else if("updateMediaDownloadingStatus".equals(action)){
			String layer = args.getString(0);
			String finished = args.getString(1);
			String total = args.getString(2);
			String finishedLayers = args.getString(3);
			String totalLayers = args.getString(4);
			
			updateMediaSyncingStatus(layer, finished, total, 
					MediaSyncingTypes.DOWNLOADING, finishedLayers, totalLayers);
		}
		
		// Returning false results in a "MethodNotFound" error.
		return false;
	}
	
	private void updateMediaSyncingStatus(final String layer, final String finished,
			final String total, final int syncType,
			final String finishedLayers, 
			final String totalLayers){
		
		final Activity activity = cordova.getActivity();
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				
				String title = activity.getResources().getString(R.string.syncing_media_title);
				String message = layer + "\n\n\t";
				
				if(syncType == MediaSyncingTypes.DOWNLOADING){
					message += activity.getResources().getString(R.string.downloaded);
					
					message += "\t" + finished + "\t/ " + total;
					
					if(mediaDownloadProgressDialog == null){
						mediaDownloadProgressDialog = ProgressDialog.show(activity, title, message, true);
					}else{
						if(finished.equals(total) && finishedLayers.equals(totalLayers)){
							mediaDownloadProgressDialog.dismiss();
						}else{
							mediaDownloadProgressDialog.setMessage(message);
						}
					}
				}else{
					message += activity.getResources().getString(R.string.uploaded);
					
					message += "\t" + finished + "\t/ " + total;
					
					if(mediaUploadProgressDialog == null){
						mediaUploadProgressDialog = ProgressDialog.show(activity, title, message, true);
					}else{
						if(finished.equals(total) && finishedLayers.equals(totalLayers)){
							mediaUploadProgressDialog.dismiss();
						}else{
							mediaUploadProgressDialog.setMessage(message);
						}
					}
				}
			}
		});
	}
	
	private void addMediaToFeature(String key, String media, String newMedia){
		FeatureDialog dialog = (FeatureDialog) getFragmentActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(FeatureDialog.TAG);
		
		dialog.updateFeaturesMedia(key, media, newMedia);
	}
	
	private void updateTileSyncingStatus(final String percentComplete){
		final String message = cordova.getActivity().getResources()
				.getString(R.string.sync_in_progress_msg);
		
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				arbiterProject.updateSyncProgressStatus(message, percentComplete);
			}
		});
	}
	
	private void createProjectTileSyncingStatus(final String percentComplete){
		final String message = cordova.getActivity().getResources()
				.getString(R.string.create_project_msg);
		
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				arbiterProject.updateProjectCreationProgressStatus(message, percentComplete);
			}
		});
	}
	
	private void errorUpdatingAOI(final String error, final CallbackContext callback){
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				arbiterProject.dismissSyncProgressDialog();
				
				Util.showDialog(cordova.getActivity(), R.string.error_updating_aoi, 
						R.string.error_updating_aoi_msg, error, null, null, null);
			}
		});
		
		callback.success();
	}
	
	// Clear the mediaToSend property in the Preferences table,
	// which keeps track of files that need to be synced
	private void clearMediaToSend(){
		CommandExecutor.runProcess(new Runnable(){
			@Override
			public void run(){
				MediaSyncHelper helper = new MediaSyncHelper(cordova.getActivity());
				helper.clearMediaToSend();
			}
		});
	}
	
	private void syncCompleted(final CallbackContext callbackContext){
		
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				arbiterProject.dismissSyncProgressDialog();
				
				clearMediaToSend();
				
				try{
					((Map.MapChangeListener) cordova.getActivity())
						.getMapChangeHelper().onSyncCompleted();
				} catch(ClassCastException e){
					e.printStackTrace();
					throw new ClassCastException(cordova.getActivity().toString() 
							+ " must be an instance of Map.MapChangeListener");
				}
				
				callbackContext.success();
			}
		});
	}
	
	private void syncFailed(final String error, final CallbackContext callback){
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				arbiterProject.dismissSyncProgressDialog();
				
				Util.showDialog(cordova.getActivity(), R.string.error_syncing, 
						R.string.error_syncing_msg, error, null, null, null);
			}
		});
		
		callback.success();
	}
	
	/**
	 * Cast the activity to a FragmentActivity
	 * @return
	 */
	private FragmentActivity getFragmentActivity(){
		FragmentActivity activity;
		
		try {
			activity = (FragmentActivity) cordova.getActivity();
		} catch (ClassCastException e){
			e.printStackTrace();
			throw new ClassCastException(cordova.getActivity().toString() 
					+ " must be an instance of FragmentActivity");
		}
		
		return activity;
	}
	
	private void featureSelected(final String featureType, final String featureId,
			final String layerId, final String wktGeometry, final String mode){
		
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				FeatureHelper helper = new FeatureHelper(getFragmentActivity());
				helper.displayFeatureDialog(featureType, featureId,
						layerId, wktGeometry, mode);
				
				if(mode.equals(ControlPanelHelper.CONTROLS.INSERT)){
					try{
						
						((Map.MapChangeListener) cordova.getActivity())
						.getMapChangeHelper().doneInsertingFeature();
						
					}catch(ClassCastException e){
						e.printStackTrace();
					}
				}
				
				notifyDoneEditingFeature();
			}
		});
	}
	
	/**
	 * Notify the MapListener that that the feature is done,
	 * being edited.
	 */
	private void notifyDoneEditingFeature(){
		try{
			// Done editing so toggle the buttons
			((Map.MapChangeListener) cordova.getActivity())
				.getMapChangeHelper().toggleEditButtons(false);
		} catch(ClassCastException e){
			e.printStackTrace();
			throw new ClassCastException(cordova.getActivity().toString() 
					+ " must be an instance of Map.MapChangeListener");
		}
	}
	
	private void doneAddingLayers(){
		arbiterProject.doneAddingLayers(cordova.getActivity().getApplicationContext());
	}
	
	private void errorAddingLayers(final String error){
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				doneAddingLayers();
				
				Util.showDialog(cordova.getActivity(), R.string.error_adding_layers, 
						R.string.error_adding_layers_msg, error, null, null, null);
			}
		});
	}
	
	private void errorLoadingFeatures(){
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				Util.showDialog(cordova.getActivity(), R.string.error_loading_features, 
						R.string.error_loading_features_msg, null, null, null, null);
			}
		});
	}
	
	private void errorCreatingProject(final CallbackContext callback){
		Log.w("ArbiterCordova", "ArbiterCordova.errorCreatingProject");
		
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				ArbiterProject.getArbiterProject().errorCreatingProject(
						cordova.getActivity());
				
				callback.success();
			}
		});
	}
	
	private void doneCreatingProject(final CallbackContext callbackContext){
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				ArbiterProject.getArbiterProject().doneCreatingProject(
						cordova.getActivity().getApplicationContext());
				
				try{
					((Map.MapChangeListener) cordova.getActivity())
						.getMapChangeHelper().onProjectCreated();
				} catch(ClassCastException e){
					e.printStackTrace();
					throw new ClassCastException(cordova.getActivity().toString() 
							+ " must be an instance of Map.MapChangeListener");
				}
				
				callbackContext.success();
			}
		});
	}
	
	private void confirmTileCount(final String count){
		
		try{
			TileConfirmation tileConfirmation = (TileConfirmation) cordova.getActivity();
			tileConfirmation.confirmTileCount(count);
		}catch(ClassCastException e){
			e.printStackTrace();
		}
	}
	
	private void setNewProjectsAOI(final String aoi, final CallbackContext callbackContext){
		//ArbiterState.getState().setNewAOI(aoi);
		ArbiterProject.getArbiterProject().getNewProject().setAOI(aoi);
		
		callbackContext.success();
		
		cordova.getActivity().finish();
	}
	
	private void showAOIConfirmationDialog(final String aoi, final String count){
		final Activity activity = cordova.getActivity();
		
		String message = activity.getResources()
				.getString(R.string.update_aoi_alert_msg);
		
		message += "\n\n" + activity.getResources().getString(
				R.string.tile_cache_warning) + " " + count;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		
		builder.setIcon(R.drawable.icon);
		builder.setTitle(R.string.update_aoi_alert);
		builder.setMessage(message);
		builder.setNegativeButton(android.R.string.cancel, null);
		
		builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				ArbiterState.getArbiterState().setNewAOI(aoi);
				
				activity.finish();
			}
		});
		
		builder.create().show();
	}
	
	/**
	 * Set the ArbiterProject Singleton's newProject aoi, commit the project, and return to the map
	 */
	private void setProjectsAOI(final String aoi, final String count){
		cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				
				showAOIConfirmationDialog(aoi, count);
			}
		});
	} 
	
	
	private void resetWebApp(final String currentExtent, final String zoomLevel,
			final CallbackContext callbackContext){
		
		final Activity activity = this.cordova.getActivity();
		final CordovaWebView webview = this.webView;
		final boolean isCreatingProject = ArbiterState
				.getArbiterState().isCreatingProject();
		
		CommandExecutor.runProcess(new Runnable(){
			@Override
			public void run(){
				OOMWorkaround oom = new OOMWorkaround(activity);
				oom.setSavedBounds(currentExtent, zoomLevel, isCreatingProject);
				
				activity.runOnUiThread(new Runnable(){
					@Override
					public void run(){
		                
						webview.loadUrl("about:blank");
					}
				});
			}
		});
	}
}
