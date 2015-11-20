package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 4/23/2015.
 */

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.utils.MessageService;


public class BaseActivity extends AppCompatActivity implements ServiceConnection {


    private MessageService.MessageServiceInterface messageServiceInterface;
    //private SinchService.SinchServiceInterface mSinchServiceInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationContext().bindService(new Intent(this, MessageService.class), this,
                BIND_AUTO_CREATE);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (MessageService.class.getName().equals(componentName.getClassName())) {
            messageServiceInterface = (MessageService.MessageServiceInterface) iBinder;
            onServiceConnected();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (MessageService.class.getName().equals(componentName.getClassName())) {
            messageServiceInterface = null;
            onServiceDisconnected();
        }
    }

    protected void onServiceConnected() {
        // for subclasses
    }

    protected void onServiceDisconnected() {
        // for subclasses
    }

    protected MessageService.MessageServiceInterface getMessageServiceInterface() {
        return messageServiceInterface;
    }
}
