package com.lmn.Arbiter_Android.Dialog.Dialogs;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.lmn.Arbiter_Android.ArbiterProject;
import com.lmn.Arbiter_Android.Dialog.ArbiterDialogFragment;
import com.lmn.Arbiter_Android.Loaders.ProjectsListLoader;

public class SwitchProjectDialog extends ArbiterDialogFragment{
	private String includeDefaultLayer;
	private String newProjectName;
	
	public static SwitchProjectDialog newInstance(String title, String ok, 
			String cancel, int layout, String newProjectName, String includeDefaultLayer){
		SwitchProjectDialog frag = new SwitchProjectDialog();
		
		frag.setTitle(title);
		frag.setOk(ok);
		frag.setCancel(cancel);
		frag.setLayout(layout);
		
		frag.includeDefaultLayer = includeDefaultLayer;
		frag.newProjectName = newProjectName;
		
		return frag;
	}
	
	@Override
	public void onPositiveClick() {
		ArbiterProject.getArbiterProject().setOpenProject(
				getActivity().getApplicationContext(),
				newProjectName, includeDefaultLayer);
		
		LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
			.sendBroadcast(new Intent(ProjectsListLoader.PROJECT_LIST_UPDATED));
	}

	@Override
	public void onNegativeClick() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCreateDialog(View view) {
		
	}
}
