package com.example.RadarHealthMonitoring;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.RadarHealthMonitoring.Archive.BLEDSG.BluetoothLeService;

import java.util.Set;
import java.util.UUID;

/**
 * Settings är en aktivitet som skapar en panel med inställningar. Innehåller genvägar till flera
 * olika paneler med olika kategorier av inställningar. Alla paneler finns under R.xml och
 * dessa styrs av aktiviteten.
 */
public class Settings extends AppCompatPreferenceActivity {

    static Settings s;

    private static final String Settingsmsg = "Settings";
    //boolean static
    //private static UUID deviceUUID;
    /* keys för olika värden från inställningarna */
    public static final String key_pref_connection_list = "connection_list";
    public static final String key_pref_usb_port = "usb_port";
    private static final int REQUEST_FINE_LOCATION = 2;
    //private static BluetoothAdapter bluetoothAdapter;

    public static UUID characteristicUuid2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        s = Settings.this;

        super.onCreate(savedInstanceState);
        setupActionBar();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();  // Skapar ett nytt fragment i en ny panel

        IntentFilter BTIntentChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverState, BTIntentChange);
        IntentFilter BTIntentFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverAction, BTIntentFound);
        IntentFilter BTIntentSearch = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearch, BTIntentSearch);
        IntentFilter BTIntentSearchFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearchFinished, BTIntentSearchFinished);
        IntentFilter BTIntentScanChange = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverScan, BTIntentScanChange);
        IntentFilter BTIntentBondChange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BluetoothSettings.BluetoothBroadcastReceiverBondChange, BTIntentBondChange);
        IntentFilter BTIntentGattConnection = new IntentFilter(BluetoothLeService.ACTION_GATT_CONNECTED); // används? Ta bort?
        registerReceiver(BluetoothSettings.gattUpdateReceiver, BTIntentBondChange);

        characteristicUuid2 = (UUID) getIntent().getSerializableExtra("extra_uuid");

        if (!hasLocationPermissions()) { // ger tillåtelse att scanna med bluetooth
            requestLocationPermission(); // TODO inaktivera scanDevice om false + ändra sammanfattning till att informera om detta
        }

        // BLE, fungerar inte
        /*final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();*/
    }

    @Override
        public void onDestroy() {
            Log.d(Settingsmsg, "onDestroy: called for Settings.");
            super.onDestroy();
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverState); // viktigt att stänga av
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverAction);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearch);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverSearchFinished);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverScan);
            unregisterReceiver(BluetoothSettings.BluetoothBroadcastReceiverBondChange);
            unregisterReceiver(BluetoothSettings.gattUpdateReceiver);
            //unbindService(service_connection);
            //bluetooth_le_adapter = null; // TODO
        }

    /**
     * Viktigt att få med alla fragment som länkas i panelerna, annars krasar appen. Är en
     * säkerhetsåtgärd för att förhindra att malware får åtkommst till fragmenten.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || Settings.SettingsFragment.class.getName().equals(fragmentName)
                || Settings.BluetoothFragment.class.getName().equals(fragmentName)
                || Settings.WifiFragment.class.getName().equals(fragmentName)
                || Settings.USBFragment.class.getName().equals(fragmentName)
                || Settings.GeneralFragment.class.getName().equals(fragmentName)
                || Settings.DeveloperFragment.class.getName().equals(fragmentName);
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
        ListPreference connectionList;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);  // hämtar preferenserna
            setHasOptionsMenu(true);  // ger menyraden
            bindPreferenceSummaryToValue(findPreference(key_pref_connection_list));  // delar värdet
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
    public static class BluetoothSettings extends PreferenceFragment {


        // Low Energy Bluetooth enable

        private static boolean discoverAll = false;
        private static boolean discoveredRaspberryPi = false;


        static BluetoothAdapter bluetoothAdapter;
        static SwitchPreference bluetoothOn;
        static ListPreference bluetoothList;
        static SwitchPreference bluetoothSearch;
        static SwitchPreference bluetoothConnect;
        static SwitchPreference bluetoothRead;
        static ConnectThread connectThread;
        private boolean mScanning;
        private static Handler handler = new Handler();

        static UUID uuid;
        static BluetoothDevice activeDevice;
        static UUID[] uuidRaspberryPi = new UUID[1];
        private static String raspberryPiMAC = "B8:27:EB:FC:22:65";

        private static Handler uiHandler = new Handler(Looper.getMainLooper());

        // ########## ########## onCreate ########## ##########

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);
            getActivity().setTitle("Bluetooth Settings");  // ändrar titeln i menyraden
            // Start Bluetooth
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Får enhetens egna Bluetooth adapter

            uuidRaspberryPi[0] = UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb");

            bluetoothOn = (SwitchPreference) findPreference("bluetooth_switch");
            bluetoothConnect = (SwitchPreference) findPreference("bluetooth_connect");
            bluetoothSearch = (SwitchPreference) findPreference("search_bluetooth_device");
            bluetoothRead = (SwitchPreference) findPreference("bluetooth_read");
            bluetoothList = (ListPreference) findPreference("bluetooth_list");
            // On create
            if (bluetoothAdapter.isEnabled()) { // kollar om Bluetooth redan är på
                bluetoothOn.setChecked(true);
                bluetoothOn.setTitle("Bluetooth On");
                enableBluetoothList();
                bluetoothConnect.setEnabled(true);
                bluetoothSearch.setEnabled(true);
                if (bluetoothAdapter.getBondedDevices().size() > 0) {
                    setActiveDevice();
                    //connectBluetooth(); // TODO automaitsk anslutning
                }
            } else {
                bluetoothOn.setChecked(false);
                bluetoothOn.setTitle("Bluetooth Off");
                bluetoothList.setEnabled(false);
                bluetoothConnect.setEnabled(false);
                bluetoothSearch.setEnabled(false);
            }
            // Ändring av enhet i Bluetoothlistan Listener
            bluetoothList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringValue = newValue.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue(stringValue);
                    listPreference.setSummary(  // ändrar summary till värdet
                            listPreference.getEntries()[index]);
                    //Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!(connectThread == null)) {
                                if (connectThread.isRunning()) {
                                    connectThread.cancel();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            "Connection canceled", Toast.LENGTH_LONG).show();
                                }
                            }
                            setActiveDevice();
                            //connectBluetooth();
                        }
                    }, 1);
                    return true;
                }
            });
            // Starta Bluetooth Swithc Listener
            bluetoothOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return startBluetooth();
                }
            });
            // Leta efter bluetoothenheter Switch Listener
            bluetoothSearch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //Log.d(Settingsmsg,newValue.toString());
                    if ((boolean) newValue) {
                        bluetoothAdapter.startDiscovery();
                        Log.d(Settingsmsg, "Enable search");
                    } else {
                        bluetoothAdapter.cancelDiscovery();
                        Log.d(Settingsmsg, "Disable search");
                    }
                    return true;
                }
            });
            // Anslut till enheten Switch Listener
            bluetoothConnect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        //BluetoothDevice.createRfcommSocketToServiceRecord();
                        if (activeDevice != null) {
                            Log.d(Settingsmsg, "run thread");
                            if (connectThread != null) {
                                if (!connectThread.hasSocket()) {
                                    connectBluetooth();
                                }
                            } else {
                                connectBluetooth();
                            }
                            connectThread.run();
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "No device selected", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(Settingsmsg, "cancel Thread");
                        connectThread.cancel();
                    }
                    return false;
                }
            });
        }
        // ########## ########## Program ########## ##########

        /**
         * Converts a 16 bit UUID to 128 bit
         * @param i
         * @return
         */
        public static UUID convertFromInteger(int i) {
            final long MSB = 0x0000000000001000L;
            final long LSB = 0x800000805f9b34fbL;
            long value = i & 0xFFFFFFFF;
            return new UUID(MSB | (value << 32), LSB);
        }

        /**
         * Method to run a thread in a static context. Can also be done with useing s.runOnUiThread
         * @param runnable
         */
        public static void runOnUI(Runnable runnable) {
            uiHandler.post(runnable);
        }

        // Vanlig Bluetooth
        private static void setActiveDevice() {
            int index = bluetoothList.findIndexOfValue(bluetoothList.getValue());
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (!(index==-1)) {
                activeDevice = (BluetoothDevice)pairedDevices.toArray()[index];
            }
        }

        public static void connectBluetooth() { // TODO Välj automatiskt Raspberrypien
            int index = bluetoothList.findIndexOfValue(bluetoothList.getValue());
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (!(index==-1)) {
                BluetoothDevice device = (BluetoothDevice)pairedDevices.toArray()[index];
                    connectThread = new ConnectThread(device);
                    Log.d(Settingsmsg, "Create Refcomm Socket: " + device.getName());
                uuid = device.getUuids()[0].getUuid();
                //Log.d(Settingsmsg, "UUID: " + uuid);
                //activeDevice = device;
                //bluetoothList.setSummary(activeDevice.getName()); // TODO
            } else {
                bluetoothList.setSummary("");
            }
            //Log.d(Settingsmsg,"No device selected");
        }

        /**
         * Startar och stänger av Bluetooth
         */
        public boolean startBluetooth() {
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                Toast.makeText(getActivity().getApplicationContext(),
                        "Bluetooth Not Supported", Toast.LENGTH_LONG).show();
                return false;
            } else {
                int REQUEST_ENABLE_BT = 1;  // okänd, måste vara större än noll
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable(); // Startar Bluetooth
                } else {
                    if (connectThread != null) {
                        if (connectThread.isRunning()) {
                            connectThread.cancel();
                        }
                    }
                    bluetoothAdapter.disable();
                }
                //Toast.makeText(getActivity().getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        /**
         * Fixar listan med Bluetoothenheter
         */
        private static void enableBluetoothList() {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                CharSequence[] deviceName = new CharSequence[pairedDevices.size()];
                CharSequence[] deviceHardwareAddress = new CharSequence[pairedDevices.size()];
                //UUID deviceUUID;
                int i = 0;
                for (BluetoothDevice device : pairedDevices) {
                    deviceName[i] = device.getName();
                    deviceHardwareAddress[i] = device.getAddress(); // MAC address
                    //deviceUUID = device.getUuids()[0].getUuid();
                    i++;
                }
                bluetoothList.setEntries(deviceName);
                bluetoothList.setEntryValues(deviceHardwareAddress);
                bluetoothList.setSummary(bluetoothList.getEntry());
                bluetoothList.setEnabled(true);
            } else {
                bluetoothList.setEnabled(false);
            }
        }

        public static UUID getUUID() {
            //UUID uuid = device.getUuids()[0].getUuid();
            return uuid;
        }

        // ########## ########## BroadcastReceivers ########## ##########

        // ########## ACTION_STATE_CHANGED ##########

        /**
         * Create a BroadcastReceiver for ACTION_STATE_CHANGED.
         */

        public static BroadcastReceiver BluetoothBroadcastReceiverState = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                    switch(state){
                        case BluetoothAdapter.STATE_OFF:
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            bluetoothOn.setChecked(false);
                            bluetoothOn.setTitle("Bluetooth Off");
                            bluetoothList.setEnabled(false);
                            bluetoothSearch.setEnabled(false);
                            bluetoothConnect.setEnabled(false);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            enableBluetoothList();
                            bluetoothSearch.setEnabled(true);
                            bluetoothConnect.setEnabled(true);
                            if (bluetoothAdapter.getBondedDevices().size()>0) {
                                //connectBluetooth(); //TODO automatisk anslutning
                            } else {
                                bluetoothList.setSummary("No device avalible");
                            }
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            bluetoothOn.setChecked(true);
                            bluetoothOn.setTitle("Bluetooth On");
                            break;
                    }
                }
            }
        };

        // ########## ACTION_FOUND ##########

        public static BroadcastReceiver BluetoothBroadcastReceiverAction = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(Settingsmsg, "Found: " + deviceName + " " + deviceHardwareAddress);
                    //Log.d(Settingsmsg,deviceName);
                    //enableBluetoothList(); TODO fixa så att listan uppdateras
                    if (deviceHardwareAddress.equals(raspberryPiMAC)) {
                        Toast.makeText(context, "Found " + deviceName, Toast.LENGTH_SHORT).show();
                        discoveredRaspberryPi=true;
                        bluetoothList.setValue(raspberryPiMAC);
                        bluetoothList.setSummary(bluetoothList.getEntry());
                        //Log.d(Settingsmsg, "Bondstate: " + device.getBondState());
                        if (device.getBondState()!=12) {
                            device.createBond();
                        }
                        //Log.d(Settingsmsg, "Bondstate: " + device.getBondState()); // Not bonded: 10, Bonding: 11, Bonded: 12

                    }
                }
            }
        };

        // ########## ACTION_DISCOVERY_FINISHED ##########

        public static BroadcastReceiver BluetoothBroadcastReceiverSearch = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //Log.d(Settingsmsg, "Started?");
                    bluetoothSearch.setChecked(true);
                }
            }
        };

        public static BroadcastReceiver BluetoothBroadcastReceiverSearchFinished = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(Settingsmsg,"Finished");
                    bluetoothSearch.setChecked(false);
                    if (!discoveredRaspberryPi) {
                        Toast.makeText(context, "Did not find Raspberry Pi", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Search Finished", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        // ########## ACTION_SCAN_MODE_CHANGED ##########

        /**
         * Broadcast Receiver for changes made to bluetooth states such as:
         * 1) Discoverability mode on/off or expire.
         */
        private static BroadcastReceiver BluetoothBroadcastReceiverScan = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                    int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                    switch (mode) {
                        //Device is in Discoverable Mode
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Discoverability Enabled.");
                            break;
                        //Device not in discoverable mode
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Connecting....");
                            break;
                        case BluetoothAdapter.STATE_CONNECTED:
                            Log.d(Settingsmsg, "mBroadcastReceiver2: Connected.");
                            break;
                    }
                }
            }
        };

        // ########## ACTION_BOND_STATE_CHANGED ##########

        /**
         * Broadcast Receiver that detects bond state changes (Pairing status changes)
         */
        private static BroadcastReceiver BluetoothBroadcastReceiverBondChange = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //3 cases:
                    //case1: bonded already
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_BONDED.");
                        enableBluetoothList();
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Log.d(Settingsmsg, "BroadcastReceiver: BOND_NONE.");
                        enableBluetoothList();
                    }
                }
            }
        };

        // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a
// result of read or notification operations.
        private static final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    //connected = true;
                    //updateConnectionState(R.string.connected);
                    //invalidateOptionsMenu();
                    bluetoothConnect.setChecked(true);
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    //connected = false;
                    //updateConnectionState(R.string.disconnected);
                    //invalidateOptionsMenu();
                    //clearUI();
                    bluetoothConnect.setChecked(false);
                } else if (BluetoothLeService.
                        ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    //displayGattServices(bluetoothLeService.getSupportedGattServices());
                    Log.d(Settingsmsg, "ACTION_GATT_SERVICES_DISCOVERED");
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        };

    } // end of BluetoothSettings

    // ########## ########## ########## Wifi ########## ########## ##########

    /**
     * Wififragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WifiFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new WifiSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * Wifiinställningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WifiSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wifi);
            setHasOptionsMenu(true);
            getActivity().setTitle("Wifi Settings");
        }
    }

    // ########## ########## ########## USB ########## ########## ##########

    /**
     * USBfragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class USBFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new USBSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * USB-inställningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class USBSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_usb);
            setHasOptionsMenu(true);
            getActivity().setTitle("USB Settings");
            bindPreferenceSummaryToValue(findPreference(key_pref_usb_port));
        }
    }

    // ########## ########## ########## General ########## ########## ##########

    /**
     * Fragment till Generella inställningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new GeneralSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * Generella instälningar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            getActivity().setTitle("General Settings");
        }
    }

    // ########## ########## ########## Developer ########## ########## ##########

    /**
     * Developerfragment
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DeveloperFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new DeveloperSettings())
                    .commit();
            setHasOptionsMenu(true);
        }
    }

    /**
     * Inställningar för utvecklare och debug
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DeveloperSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_developer);
            setHasOptionsMenu(true);
            getActivity().setTitle("Developer Settings");
        }
    }

    // ########## ########## ########## ########## ########## ########## ##########
    // End of Settings Fragment. Start of shared preferences and listeners
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
                if (listPreference.getKey().equals(key_pref_connection_list)) {  // ändrar ikonen
                    switch (stringValue) {
                        case "1":
                            listPreference.setIcon(R.drawable.ic_bluetooth_black_24dp);
                            break;
                        case "0":
                            listPreference.setIcon(R.drawable.ic_wifi_black_24dp);
                            break;
                        case "-1":
                            listPreference.setIcon(R.drawable.ic_usb_black_24dp);
                            break;
                    }
                }
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

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        // Trigger the listener immediately with the preference's current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        //log("Requested user enable Location. Try starting the scan again.");
    }
} // end of Settings.class
