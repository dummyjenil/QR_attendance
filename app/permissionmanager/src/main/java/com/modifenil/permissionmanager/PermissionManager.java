package com.modifenil.permissionmanager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class PermissionManager {
    private Activity activity;
    public void checkAndRequestPermissions(Activity activity) {
        this.activity=activity;
        List<String> listPermissionsNeeded = setPermission();
        List<String> listPermissionsAssign=new ArrayList<>();
        for(String per:listPermissionsNeeded){
            if(ContextCompat.checkSelfPermission(activity.getApplicationContext(),per)!=PackageManager.PERMISSION_GRANTED){
                listPermissionsAssign.add(per);
            }
        }

        if (!listPermissionsAssign.isEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsAssign.toArray(new String[0]),1212);
        }
    }

    public ArrayList<statusArray> getStatus(){
        ArrayList<statusArray> statusPermission=new ArrayList<>();
        ArrayList<String> grant=new ArrayList<>();
        ArrayList<String> deny=new ArrayList<>();
        List<String> listPermissionsNeeded = setPermission();
        for(String per:listPermissionsNeeded){
            if(ContextCompat.checkSelfPermission(activity.getApplicationContext(),per)==PackageManager.PERMISSION_GRANTED){
                grant.add(per);
            }else{
                deny.add(per);
            }
        }
        statusArray stat = new statusArray(grant, deny);
        statusPermission.add(stat);
        return statusPermission;
    }

    public List<String>  setPermission() {
        List<String> per=new ArrayList<>();
        try {
            PackageManager pm = activity.getApplicationContext().getPackageManager();
            PackageInfo pi=pm.getPackageInfo(activity.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissionInfo =pi.requestedPermissions;
            Collections.addAll(per, permissionInfo);
        } catch (Exception ignored) {

        }
        return per;
    }

    public void checkResult(int requestCode, String[] permissions, int[] grantResults){
        if (requestCode == 1212) {
            List<String> listPermissionsNeeded = setPermission();
            Map<String, Integer> perms = new HashMap<>();
            for (String permission : listPermissionsNeeded) {
                perms.put(permission, PackageManager.PERMISSION_GRANTED);
            }
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                boolean isAllGranted = true;
                for (String permission : listPermissionsNeeded) {
                    if (Optional.ofNullable(perms.get(permission)).orElse(0) == PackageManager.PERMISSION_DENIED) {
                        isAllGranted = false;
                        break;
                    }
                }
                if (!isAllGranted) {
                    boolean shouldRequest = false;
                    for (String permission : listPermissionsNeeded) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            shouldRequest = true;
                            break;
                        }
                    }
                    if (shouldRequest) {
                        ifCancelledAndCanRequest(activity);
                    } else {
                        ifCancelledAndCannotRequest(activity);
                    }
                }
            }
        }
    }

    public void ifCancelledAndCanRequest(final Activity activity) {

        showDialogOK(activity,
                (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            checkAndRequestPermissions(activity);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            // proceed with logic by disabling the related features or quit the app.
                            break;
                    }
                });
    }
    public void ifCancelledAndCannotRequest(Activity activity){
        Toast.makeText(activity.getApplicationContext(), "Go to settings and enable permissions", Toast.LENGTH_LONG).show();
    }
    private void showDialogOK(Activity activity, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(activity)
                .setMessage("Some Permission required for this app, please grant permission for the same")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }
    public static class statusArray{
        statusArray(ArrayList<String> granted,ArrayList<String> denied){
            this.denied=denied;
            this.granted=granted;
        }
        public ArrayList<String> granted;
        public ArrayList<String> denied;
    }
}

