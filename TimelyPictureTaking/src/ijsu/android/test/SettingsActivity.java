package ijsu.android.test;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	private EditTextPreference ipPreference, portPreference, delayPreference, periodPreference;
	private ListPreference listPreference;
	private CharSequence[] lstEntries = {"320x240","640x480"};
	private CharSequence[] lstValues = {"320x240","640x480"};
//	private String serverIp, serverPort, timeDelay, timePeriod, pictureSize;
    SharedPreferences settings; 
    PreferenceManager manager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        manager = getPreferenceManager();
        listPreference = (ListPreference) manager.findPreference("pictureSize");
        ipPreference = (EditTextPreference) manager.findPreference("serverIp");
        portPreference = (EditTextPreference) manager.findPreference("serverPort");
        delayPreference = (EditTextPreference) manager.findPreference("timeDelay");
        periodPreference = (EditTextPreference) manager.findPreference("timeInterval");
        listPreference.setEntries(lstEntries);
        listPreference.setEntryValues(lstValues);
        ipPreference.setSummary("\t 目前設定: " + settings.getString("serverIp", ""));
        portPreference.setSummary("\t 目前設定: " + settings.getString("serverPort", ""));
        delayPreference.setSummary("\t 目前設定: " + settings.getString("timeDelay", ""));
        periodPreference.setSummary("\t 目前設定: " + settings.getString("timeInterval", ""));
        listPreference.setSummary("\t 目前設定: " + settings.getString("pictureSize", ""));       
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // Set up a listener whenever a key changes            
        settings.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes            
        settings.unregisterOnSharedPreferenceChangeListener(this);    
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary("\t 目前設定: " + listPref.getEntry());
        } 
        if (pref instanceof EditTextPreference) {
            EditTextPreference etPref = (EditTextPreference) pref;
            pref.setSummary("\t 目前設定: " + etPref.getText());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	return true;
    }

}
