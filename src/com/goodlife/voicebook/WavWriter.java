package com.goodlife.voicebook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

public class WavWriter {

    private static final String TAG = "WavWriter";

    public static void produceWaveFile(String outputPath, int[][] data, int sampleRate, int byteRate)
            throws IOException {
        int channels = data[0].length;
        Log.d(TAG, "" + channels + "channel(s)");
        int sampleNum = data.length;
        Log.d(TAG, "" + sampleNum + "sample(s)");
        int audioLen = sampleNum * channels * 2;
        byte[] dataToBeWritten = new byte[audioLen];
        // the space of a short integer is equal to that of two bytes
        FileOutputStream fos = new FileOutputStream(outputPath);
        writeWaveFileHeader(fos, audioLen, sampleRate, channels, byteRate);
        for (int i = 0; i < sampleNum; i++) {
            for (int j = 0; j < channels; j++) {
                int datum = data[i][j];
                dataToBeWritten[2 * (i * channels + j)] = (byte) (datum & 0xff);
                dataToBeWritten[2 * (i * channels + j) + 1] = (byte) ((datum >> 8) & 0xff);
            }
        }
        fos.write(dataToBeWritten);
        fos.close();
    }

    public static void appendData(String outputPath, int[][] data) throws IOException {
        int channels = data[0].length;
        Log.d(TAG, "" + channels + "channel(s)");
        int sampleNum = data.length;
        Log.d(TAG, "" + sampleNum + "sample(s)");
        int audioLen = sampleNum * channels * 2;
        byte[] dataToBeWritten = new byte[audioLen];
        FileOutputStream fos = new FileOutputStream(outputPath);
        for (int i = 0; i < sampleNum; i++) {
            for (int j = 0; j < channels; j++) {
                int datum = data[i][j];
                dataToBeWritten[2 * (i * channels + j)] = (byte) (datum & 0xff);
                dataToBeWritten[2 * (i * channels + j) + 1] = (byte) ((datum >> 8) & 0xff);
            }
        }
        fos.write(dataToBeWritten);
        fos.close();
    }

    public static void appendData(String outputPath, short[][] data) throws IOException {
        int channels = data[0].length;
        Log.d(TAG, "" + channels + "channel(s)");
        int sampleNum = data.length;
        Log.d(TAG, "" + sampleNum + "sample(s)");
        int audioLen = sampleNum * channels * 2;
        byte[] dataToBeWritten = new byte[audioLen];
        FileOutputStream fos = new FileOutputStream(outputPath);
        for (int i = 0; i < sampleNum; i++) {
            for (int j = 0; j < channels; j++) {
                int datum = data[i][j];
                dataToBeWritten[2 * (i * channels + j)] = (byte) (datum & 0xff);
                dataToBeWritten[2 * (i * channels + j) + 1] = (byte) ((datum >> 8) & 0xff);
            }
        }
        fos.write(dataToBeWritten);
        fos.close();
    }

    public static void copyWaveFile(String inFile, String outFile, long samplingRate, int bufferSize,
            boolean isDeleteOrigin) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long audioLen;
        long sampleRate = samplingRate;
        int channels = 1;
        long byteRate = 2 * sampleRate * channels;
        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFile);
            out = new FileOutputStream(outFile);
            audioLen = in.getChannel().size();
            writeWaveFileHeader(out, audioLen, sampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            File inputFile = new File(inFile);
            in.close();
            out.close();
            if (isDeleteOrigin)
                inputFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyWaveFile(String inFile, String outFile, long samplingRate, int bufferSize) {
        copyWaveFile(inFile, outFile, samplingRate, bufferSize, true);
    }

    public static void writeWaveFileHeader(FileOutputStream out, long audioLen, long sampleRate, int channels,
            long byteRate) throws IOException {
        byte[] header = new byte[44];
        long dataLen = audioLen + 36;
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (dataLen & 0xff);
        header[5] = (byte) ((dataLen >> 8) & 0xff);
        header[6] = (byte) ((dataLen >> 16) & 0xff);
        header[7] = (byte) ((dataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (byteRate / sampleRate); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (audioLen & 0xff);
        header[41] = (byte) ((audioLen >> 8) & 0xff);
        header[42] = (byte) ((audioLen >> 16) & 0xff);
        header[43] = (byte) ((audioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
