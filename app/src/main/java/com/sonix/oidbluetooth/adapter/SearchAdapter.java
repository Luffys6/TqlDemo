package com.sonix.oidbluetooth.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sonix.oidbluetooth.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private LayoutInflater inflater;
    private OnItemClickListener listener;

    private ArrayList<String> addresses;
    private ArrayList<BluetoothDevice> devices;

    public SearchAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        devices = new ArrayList<>();
        addresses = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(inflater.inflate(R.layout.item_search, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder mHolder = (ViewHolder) viewHolder;
        mHolder.name.setText(context.getString(R.string.pen_name, devices.get(i).getName()));
        mHolder.address.setText(context.getString(R.string.pen_address, devices.get(i).getAddress()));
        mHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null && devices.size()>0)
                    listener.onItemClick(devices.get(i));
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout item;
        private TextView name, address;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.ll_item);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
        }
    }
    /*private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private LinearLayout item;
        private TextView name, address;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            item = itemView.findViewById(R.id.ll_item);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
        }

        @Override
        public void onClick(View v) {
            if (listener != null && getAdapterPosition() >= 0)
                listener.onItemClick(devices.get(getAdapterPosition()));
        }
    }*/

    /**
     * Byte数组转String
     *
     * @param bytes 字节数组
     * @return 16进制字符串
     */
    private String bytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String plainText = Integer.toHexString(0xFF & b);
            if (plainText.length() < 2)
                plainText = "0" + plainText;
            hexString.append(plainText);
        }
        return hexString.toString().toUpperCase();
    }

    /**
     * SmartPen过滤
     *
     * @param scanRecord 蓝牙返回字节数组
     * @return 是否为SmartPen
     */
    private boolean isToneSmartPen(byte[] scanRecord) {
        String ret = bytesToHexString(scanRecord);
        return ret.contains("FF31323334");
    }

    public void clearDevice() {
        devices.clear();
        addresses.clear();
        notifyDataSetChanged();
    }

    public void addDevice(BluetoothDevice device, byte[] scanRecord) {
        if (!isToneSmartPen(scanRecord))
            return;
        if (addresses.contains(device.getAddress())) {
            devices.set(addresses.indexOf(device.getAddress()), device);
            return;
        }

        devices.add(device);
        addresses.add(device.getAddress());

        notifyDataSetChanged();
    }

    /**
     * 解析笔的名字
     *
     * @param scanRecord
     * @return
     */
    private String parseDeviceName(byte[] scanRecord) {
        String ret = null;
        if (null == scanRecord) {
            return ret;
        }

        ByteBuffer buffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0)
                break;

            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    buffer.get(); // flags
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        buffer.getShort();
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        buffer.getInt();
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    ret = new String(sb).trim();
                    return ret;
                case (byte) 0xFF: // Manufacturer Specific Data
                    buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                buffer.position(buffer.position() + length);
            }
        }
        return ret;
    }


    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
