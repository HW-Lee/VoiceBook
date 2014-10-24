package com.goodlife.voicebook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

public class WavReader {

    private final String TAG = "WavReader";

    private File mFile = null;
    private FileInputStream fin = null;
    private boolean isWavFile = false;
    private int dataLen = 0;
    private int channels = 0;
    private int sampleRate = 0;
    private int blockAlign = 0;
    private int bitPerSample = 0;
    private int chunkSize = 0;
    private int tag = 0;
    private int sampleNum = 0;
    private boolean isInitialized = false;
    private byte[] data = null;

    public WavReader(String path) throws IOException {
        this(new File(path));
    }

    public WavReader(File file) throws IOException {
        this.mFile = file;
    }

    public boolean isWavFile() {
        return isWavFile;
    }

    public long getDataLength() {
        return dataLen;
    }

    public int getSampleNum() {
        return sampleNum;
    }

    public int getChannelsNum() {
        return channels;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public int getBlockAlign() {
        return blockAlign;
    }

    public int getBitPerSample() {
        return bitPerSample;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getTagID() {
        return tag;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public File getFile() {
        return mFile;
    }

    public FileInputStream getInputStream() {
        return fin;
    }

    public short[] retrieveShortArray(int ind_start, int bufferSize) throws IOException {
        return ByteArr2ShortArr(retrieveByteArray(ind_start, 2 * bufferSize));
    }

    public byte[] retrieveByteArray(int ind_start, int bufferSize) throws IOException {
        int start, size;
        size = (bufferSize / channels) * channels;
        byte[] byteArr = new byte[size];
        start = (ind_start < 0) ? 0 : ind_start;
        for (int i = 0; i < size; i++)
            if (i + 2 * start < this.data.length)
                byteArr[i] = data[i + 2 * start];
            else
                byteArr[i] = 0;
        return byteArr;
    }

    private short[] ByteArr2ShortArr(byte[] input) {
        short[] output = new short[input.length / 2];
        for (int i = 0; i < input.length / 2; i++) {
            output[i] = (short) ((input[2 * i] & 0xff) | (input[2 * i + 1] << 8));
        }
        return output;
    }

    public void initialize() throws IOException {
        FileInputStream target = new FileInputStream(mFile);
        byte[] header = new byte[44];
        byte[] intArr = new byte[4];
        target.read(header);
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
                && header[9] == 'A' && header[10] == 'V' && header[11] == 'E' && header[12] == 'f' && header[13] == 'm'
                && header[14] == 't' && header[15] == ' ' && header[36] == 'd' && header[37] == 'a'
                && header[38] == 't' && header[39] == 'a') {
            for (int i = 0; i < 4; i++) {
                intArr[i] = header[7 - i];
            }
            this.dataLen = bytesArr2Int(intArr);
            for (int i = 0; i < 4; i++) {
                intArr[i] = header[20 - i];
            }
            this.tag = bytesArr2Int(intArr);
            for (int i = 0; i < 4; i++) {
                intArr[i] = header[27 - i];
            }
            this.sampleRate = bytesArr2Int(intArr);
            this.channels = header[22];
            this.chunkSize = header[16];
            this.blockAlign = (header[32] & 0xff) | (header[33] & 0xff) << 8;
            this.bitPerSample = (header[34] & 0xff) | (header[35] & 0xff) << 8;
            this.isWavFile = true;
        } else {
            this.isWavFile = false;
            isInitialized = true;
            return;
        }
        long audioLen = this.dataLen - 36;
        this.sampleNum = (int) (audioLen / this.blockAlign);
        isInitialized = true;
        this.fin = target;
        this.data = new byte[(int) audioLen];
        this.fin.read(data);
        target.close();
    }

    private int bytesArr2Int(byte[] arr) {
        if (arr.length == 4) {
            return (arr[0] & 0xff) << 24 | (arr[1] & 0xff) << 16 | (arr[2] & 0xff) << 8 | (arr[3] & 0xff);
        } else {
            return 0;
        }
    }

    public void info() {
        Log.i(TAG, "isWaveFile? " + this.isWavFile);
        Log.i(TAG, "Sampling Rate: " + this.sampleRate + " Hz");
        Log.i(TAG, "" + this.sampleNum + " sample(s)");
        Log.i(TAG, "" + this.channels + " channel(s)");
        Log.i(TAG, "" + this.data.length + " byte(s) of the data");
    }
}
