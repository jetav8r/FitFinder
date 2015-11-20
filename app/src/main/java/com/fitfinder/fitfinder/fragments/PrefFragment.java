package com.fitfinder.fitfinder.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fitfinder.fitfinder.R;

/**
 * Created by Wayne on 7/24/2015.
 */
public class PrefFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    SharedPreferences prefs;

    public static PrefFragment newInstance() {
        PrefFragment fragment = new PrefFragment();

        return fragment;
    }

    public PrefFragment() {
        // Required empty public constructor
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        String id = (String) preference.getTitle();
        Log.e("MyApp", "key = " + key + ", id =" + id);

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;
        view.setBackgroundColor(getResources().getColor(android.R.color.white));
        addPreferencesFromResource(R.xml.prefs);
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = newValue.toString();
        Log.e("MyApp", "new value = " + value + " , " + newValue);
        return true;
    }

}