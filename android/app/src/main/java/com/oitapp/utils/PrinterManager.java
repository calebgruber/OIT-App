    package com.oitapp.utils;

    import android.bluetooth.BluetoothAdapter;
    import android.bluetooth.BluetoothDevice;
    import android.bluetooth.BluetoothSocket;
    import android.text.TextUtils;
    import java.io.IOException;
    import java.io.OutputStream;
    import java.net.InetSocketAddress;
    import java.net.Socket;
    import java.nio.charset.StandardCharsets;
    import java.util.Set;
    import java.util.UUID;

    public class PrinterManager {
        private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private Socket wifiSocket;
        private BluetoothSocket bluetoothSocket;
        private OutputStream outputStream;

        public boolean connectBluetooth(String address) {
            disconnect();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || TextUtils.isEmpty(address)) {
                return false;
            }
            try {
                Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                for (BluetoothDevice device : bondedDevices) {
                    if (address.equalsIgnoreCase(device.getAddress())) {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        return true;
                    }
                }
            } catch (IOException exception) {
                disconnect();
            }
            return false;
        }

        public boolean connectWifi(String ipAddress, int port) {
            disconnect();
            if (TextUtils.isEmpty(ipAddress)) {
                return false;
            }
            try {
                wifiSocket = new Socket();
                wifiSocket.connect(new InetSocketAddress(ipAddress, port > 0 ? port : 9100), 5000);
                outputStream = wifiSocket.getOutputStream();
                return true;
            } catch (IOException exception) {
                disconnect();
                return false;
            }
        }

        public boolean printLabel(LabelData labelData) {
            if (!isConnected() || labelData == null) {
                return false;
            }
            try {
                outputStream.write(buildZpl(labelData).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                return true;
            } catch (IOException exception) {
                disconnect();
                return false;
            }
        }

        public boolean isConnected() {
            return outputStream != null;
        }

        public void disconnect() {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (wifiSocket != null) {
                    wifiSocket.close();
                }
            } catch (IOException ignored) {
            }
            outputStream = null;
            bluetoothSocket = null;
            wifiSocket = null;
        }

        private String buildZpl(LabelData data) {
            return "^XA\n"
                    + "^CF0,32\n"
                    + "^FO40,40^FD" + safe(data.patientName) + "^FS\n"
                    + "^FO40,80^FDMRN: " + safe(data.mrn) + "^FS\n"
                    + "^FO40,120^FDDose: " + safe(data.doseAmount) + " " + safe(data.doseForm) + "^FS\n"
                    + "^FO40,160^FDDraw: " + safe(data.drawDate) + "^FS\n"
                    + "^FO40,200^FDSched: " + safe(data.scheduledDate) + "^FS\n"
                    + "^FO40,240^FDExp: " + safe(data.finalExpiry) + "^FS\n"
                    + "^FO40,280^FDCarton: " + safe(data.cartonLot) + "^FS\n"
                    + "^FO40,320^FDSeal: " + safe(data.sealType) + "^FS\n"
                    + "^BY2,3,70\n"
                    + "^FO40,360^BCN,80,Y,N,N^FD" + safe(data.syringeBarcode) + "^FS\n"
                    + "^XZ\n";
        }

        private String safe(String value) {
            return value == null ? "" : value.replace("^", " ").replace("~", " ");
        }

        public static class LabelData {
            public String patientName;
            public String mrn;
            public String syringeBarcode;
            public String doseAmount;
            public String doseForm;
            public String drawDate;
            public String scheduledDate;
            public String finalExpiry;
            public String cartonLot;
            public String cartonExpiry;
            public String sealType;
        }
    }
