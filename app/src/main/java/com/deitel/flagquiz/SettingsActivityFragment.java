package com.deitel.flagquiz;

import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {
    //subclass of PreferenceFragment for managing app settings

    //creates preferences GUI from preferences.xml file in res/xml
    //As the user interacts with preferences GUI, the preferences
    //are auto stored into a SharedPreferences file on the device.

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        //load from XML
        addPreferencesFromResource(R.xml.preferences);
    }
}
