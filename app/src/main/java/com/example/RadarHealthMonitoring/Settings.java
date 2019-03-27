package com.example.RadarHealthMonitoring;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.example.RadarHealthMonitoring.Bluetooth.START_MEAS_BUTTON_ENABLE;
import static com.example.RadarHealthMonitoring.Bluetooth.b;

/**
 * Settings är en aktivitet som skapar en panel med inställningar. Innehåller genvägar till flera
 * olika paneler med olika kategorier av inställningar. Alla paneler finns under R.xml och
 * dessa styrs av aktiviteten.
 */
public class Settings extends AppCompatPreferenceActivity {

    static Settings s; // for static activity
    private static final String Settingsmsg = "Settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Settingsmsg, "onCreate for Settings");
        s = Settings.this;
        setupActionBar();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();  // Skapar ett nytt fragment i en ny panel
    }

    @Override
        public void onDestroy() {
            Log.d(Settingsmsg, "onDestroy: called for Settings.");
            super.onDestroy();
        }

    /**
     * Viktigt att få med alla fragment som länkas i panelerna, annars krasar appen. Är en
     * säkerhetsåtgärd för att förhindra att malware får åtkommst till fragmenten.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.class.getName().equals(fragmentName)
                || Settings.BluetoothFragment.class.getName().equals(fragmentName);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  // Ger en fungerande tillbaka-pil
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    // ########## ########## ########## ########## ########## ########## ##########
    // Start of Settings Fragments
    // ########## ########## ########## ########## ########## ########## ##########

    /**
     * Huvudpanelen för Settings. Innehåller både en lista där anslutning väljs.
     * Innehåller även länkar till de nestade framgenten/panelerna.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);  // hämtar preferenserna
            setHasOptionsMenu(true);  // ger menyraden
        }
    }

    // ########## ########## ########## Bluetooth ########## ########## ##########

    /**
     * Bluetoothfragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new BluetoothSettings()) // add?
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     *  Inställningar för Bluetooth
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothSettings extends PreferenceFragment implements EasyPermissions.PermissionCallbacks {

        static SwitchPreference bluetoothOn;
        static SwitchPreference bluetoothAutoConnect;
        static ListPreference bluetoothList;
        static SwitchPreference bluetoothSearch;
        static SwitchPreference bluetoothConnect;
        static EditTextPreference bluetoothRaspberryPiName;
        static EditTextPreference commandTerminal;

        private static final String key_pref_bluetooth_switch = "bluetooth_switch";
        private static final String key_pref_bluetooth_auto_connect = "bluetooth_auto_connect";
        private static final String key_pref_bluetooth_connect = "bluetooth_connect";
        private static final String key_pref_bluetooth_search = "search_bluetooth_device";
        private static final String key_pref_bluetooth_list = "bluetooth_list";
        private static final String key_pref_raspberry_pi_name = "bluetooth_raspberrypi_name";
        private static final String key_pref_command_terminal = "command_terminal";


        static final String RESET_GRAPH = "RESET_GRAPH";

        static BluetoothSettings bs; // for static service
        private static Handler uiHandler = new Handler(Looper.getMainLooper()); // static handler

        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(Settingsmsg, "BluetoothSettings onCreate");
            bs = BluetoothSettings.this;
            b.bluetoothSettingsActive = true;
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle("Bluetooth Settings");  // Change title
            // Bluetooth preferences
            bluetoothOn = (SwitchPreference) findPreference(key_pref_bluetooth_switch);
            bluetoothAutoConnect = (SwitchPreference) findPreference(key_pref_bluetooth_auto_connect);
            bluetoothConnect = (SwitchPreference) findPreference(key_pref_bluetooth_connect);
            bluetoothSearch = (SwitchPreference) findPreference(key_pref_bluetooth_search);
            bluetoothList = (ListPreference) findPreference(key_pref_bluetooth_list);
            bluetoothRaspberryPiName = (EditTextPreference) findPreference(key_pref_raspberry_pi_name);
            commandTerminal = (EditTextPreference) findPreference(key_pref_command_terminal);
            // On return to bluetooth settings, manually update everything
            if (b.bluetoothOnChecked) {
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle("Bluetooth On");
                b.updateBluetoothList();
            } else {
                bluetoothOn.setChecked(false);
                bluetoothOn.setTitle("Bluetooth Off");
            }
            bluetoothConnect.setEnabled(b.bluetoothConnectEnable);
            bluetoothConnect.setChecked(b.bluetoothConnectChecked);
            bluetoothAutoConnect.setEnabled(b.bluetoothConnectEnable);
            bluetoothAutoConnect.setChecked(b.bluetoothAutoConnectChecked);
            bluetoothSearch.setEnabled(b.bluetoothSearchEnable);
            bluetoothSearch.setChecked(b.bluetoothSearchChecked);
            if (!b.commandBluetoothList) {
                getPreferenceScreen().removePreference(bluetoothList);
            }
            bluetoothList.setEnabled(b.bluetoothListEnable && b.commandBluetoothList);
            //bluetoothWrite.setEnabled(b.bluetoothWriteEnable);

            // ########## Preference Listeners ##########

            // Starta Bluetooth Swithc Listener
            bluetoothOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return b.startBluetooth((boolean)newValue);
                }
            });

            // Auto connect switch listener
            bluetoothAutoConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        b.autoConnect = true;
                        b.autoConnect();
                        return false;
                    } else {
                        b.autoConnect = false;
                        b.bluetoothAdapter.cancelDiscovery();
                        if (b.connectThread != null) {
                            Log.d(Settingsmsg, "cancel Thread");
                            b.connectThread.cancel();
                        } else {
                            b.bluetoothAutoConnectChecked = false;
                            return true;
                        }
                    }
                    return false;
                }
            });

            // Change Raspberry Pi bluetooth name or MAC address text preference
            bluetoothRaspberryPiName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    b.raspberryPiName = (String) newValue;
                    preference.setSummary((String)newValue);
                    return true;
                }
            });

            // Ändring av enhet i Bluetoothlistan Listener
            bluetoothList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringValue = newValue.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    b.chosenDeviceIndex = listPreference.findIndexOfValue(stringValue);
                    listPreference.setSummary(listPreference.getEntries()[b.chosenDeviceIndex]); // ändrar summary till värdet
                    b.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!(b.connectThread == null)) {
                                if (b.connectThread.isRunning()) {
                                    b.connectThread.cancel();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            "Connection canceled", Toast.LENGTH_LONG).show();
                                }
                            }
                            b.setActiveDevice();
                        }
                    }, 1); // delay needed, otherwise it won't change
                    return true;
                }
            });

            // Leta efter bluetoothenheter Switch Listener
            bluetoothSearch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        b.searchAttempts = 1;
                        b.autoConnect = false;
                        b.startDiscovery();
                    } else {
                        b.bluetoothAdapter.cancelDiscovery();
                        Log.d(Settingsmsg, "Disable search");
                        b.autoConnect = false;
                        if (!b.connected) {
                            b.bluetoothAutoConnectChecked = false;
                            bluetoothAutoConnect.setChecked(false);
                        }
                        return true;
                    }
                    return false;
                }
            });

            // Anslut till enheten Switch Listener
            bluetoothConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        if (b.activeDevice != null) {
                            b.autoConnect = false;
                            b.connectAttempt = 4;
                            b.connectBluetooth(true);
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "No device selected", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (b.connectThread != null) {
                            Log.d(Settingsmsg, "cancel Thread");
                            b.connectThread.cancel();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });

            // Write to  Raspberry Pi text preference
            commandTerminal.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String command = ((String)newValue).toLowerCase();
                    Log.d(Settingsmsg, "Command: " + command);
                    switch (command) {
                        case "poweroff":
                            if (b.connected) {
                                byte[] sendCommand = command.getBytes();
                                b.connectedThread.write(sendCommand);
                                preference.setSummary((String)newValue);
                                Toast.makeText(getActivity().getApplicationContext(), "Power off Raspberry Pi", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity().getApplicationContext(), "Not connected to Raspberry Pi", Toast.LENGTH_LONG).show();
                            }
                            break;
                        case "list":
                            if (b.commandBluetoothList) {
                                b.commandBluetoothList = false;
                                b.bluetoothListEnable = false;
                                bluetoothList.setEnabled(false);
                                getPreferenceScreen().removePreference(bluetoothList);
                            } else {
                                b.commandBluetoothList = true;
                                getPreferenceScreen().addPreference(bluetoothList);
                                b.updateBluetoothList();
                                if (b.bluetoothListEnable) {
                                    bluetoothList.setEnabled(true);
                                }
                            }
                            break;
                        case "simulate":
                            b.commandSimulate = !b.commandSimulate;
                            Intent StartMeasButtonIntent = new Intent(START_MEAS_BUTTON_ENABLE);
                            StartMeasButtonIntent.putExtra(b.ENABLE_BUTTON,b.commandSimulate || b.connected);
                            s.sendBroadcast(StartMeasButtonIntent);
                            Intent readIntent = new Intent(BluetoothSettings.RESET_GRAPH);
                            s.sendBroadcast(readIntent);
                            break;
                        default:
                            Toast.makeText(getActivity().getApplicationContext(), "Not a command", Toast.LENGTH_LONG).show();
                            break;
                    }
                    return true;
                }
            });
        } // end of onCreate

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(Settingsmsg, "BluetoothSettings onDestroy");
            b.bluetoothSettingsActive = false;
        }

        // ########## ########## Methods ########## ##########


        // ########## ########## Request Permission ########## ##########
        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }

        @Override
        public void onPermissionsGranted(int requestCode, List<String> list) {
            // Some permissions have been granted
            if (requestCode == 2) {
                b.startDiscovery();
            }
        }

        @Override
        public void onPermissionsDenied(int requestCode, List<String> list) {
            // Some permissions have been denied
            if (requestCode == 2) {
                b.bluetoothAutoConnectChecked = false;
                Settings.BluetoothSettings.bluetoothAutoConnect.setChecked(false);
                Toast.makeText(getActivity().getApplicationContext(), "Location Permissions denied", Toast.LENGTH_LONG).show();
            }
        }

    } // end of BluetoothSettings


    // ########## ########## ########## ########## ########## ########## ##########
    // End of Settings Fragments. Start of shared preferences and listeners
    // ########## ########## ########## ########## ########## ########## ##########

    /**
     * Lyssnar efter ändringar. Gör bland annat att alla preferenser som har en key och är bundet med
     * bindPreferenceSummaryToValue kan få sitt värde utskrivet i summary. Dessutom får listan
     * med olika anslutningsmöjligheter sina figurer här.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(  // ändrar summary till värdet
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) { // TODO undersök om behövs
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        // Trigger the listener immediately with the preference's current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
} // end of Settings.class
