package com.goodlife.voicebook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int SAMPLING_RATE = 16000;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final String TAG = "VoiceBook";

    private Button modeSwitch = null;
    private Button addTagBtn = null;
    private Button playBackRateXBtn = null;
    private Button play_record_Btn = null;
    private Button stopBtn = null;
    private Button readDataBtn = null;
    private EditText playBackRateXEdt = null;
    private EditText fileNameEdt = null;
    private TextView timeTxt = null;
    private ListView tagList = null;
    private TagListAdapter myAdapter = null;
    ArrayList<Integer> tagIndList = null;

    private boolean useMode = true; // Recording:Playing
    private boolean isRecording = false;
    private boolean isPlaying = false;

    private float playBackRateX = 1;
    private int bufferSize = 0;
    private double currentTimeBySec = 0;
    private int indPlaying = 0;
    private int indRecording = 0;

    private Handler myHandler = null;
    private File SDCardPath = null;
    private AudioRecord myRecorder = null;
    private AudioTrack myPlayer = null;
    private WavReader mWaveReader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        initSetting();
    }

    private void initSetting() {
        // Get Refs
        modeSwitch = (Button) findViewById(R.id.modeSwitch);
        addTagBtn = (Button) findViewById(R.id.addTag);
        playBackRateXBtn = (Button) findViewById(R.id.playBackRateXBtn);
        play_record_Btn = (Button) findViewById(R.id.record_play_btn);
        stopBtn = (Button) findViewById(R.id.stopbtn);
        readDataBtn = (Button) findViewById(R.id.readData);
        playBackRateXEdt = (EditText) findViewById(R.id.playBackRateXEdt);
        fileNameEdt = (EditText) findViewById(R.id.fileName);
        timeTxt = (TextView) findViewById(R.id.time);
        tagList = (ListView) findViewById(R.id.tagList);

        // Set Texts
        modeSwitch.setText("Recording");
        addTagBtn.setText("Add Tag");
        play_record_Btn.setText("Record");
        playBackRateXBtn.setText("Check");
        stopBtn.setText("Stop");
        readDataBtn.setText("Read Data");
        playBackRateXEdt.setText("1.0");
        timeTxt.setText("Time: " + parseTimeBySec(0));

        // Set Listeners
        modeSwitch.setOnClickListener(mListener);
        addTagBtn.setOnClickListener(mListener);
        playBackRateXBtn.setOnClickListener(mListener);
        play_record_Btn.setOnClickListener(mListener);
        stopBtn.setOnClickListener(mListener);
        readDataBtn.setOnClickListener(mListener);

        // Check if there is an external storage
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)) {
            Log.i(TAG, "ExternalStorage exists!");
            SDCardPath = Environment.getExternalStorageDirectory();
            File myDataPath = new File(SDCardPath.getAbsolutePath() + "/VoiceBook");
            if (!myDataPath.exists())
                myDataPath.mkdirs();
        } else {
            Log.i(TAG, "ExternalStorage does not exist!");
        }

        bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNELS, AUDIO_ENCODING);

        // Handler
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj instanceof Double) {
                    String modeStr = (useMode ? "Recording " : "Playing ") + "time: ";
                    timeTxt.setText(modeStr + parseTimeBySec((Double) msg.obj));
                } else if (msg.obj instanceof String) {
                    if (msg.obj.equals("Jump")) {
                        Log.d(TAG, "Jump to the index " + msg.what);
                        if (!useMode)
                            indPlaying = msg.what;
                    }
                    if (msg.what == 0) {
                        boolean[] sets = { false, true, false };
                        if (msg.obj.equals(String.valueOf("Stop")))
                            setAllButton(sets);
                        else
                            setAllButton(true);
                    } else if (msg.what == 1) {
                        readDataBtn.setText((CharSequence) msg.obj);
                    }
                } else if (msg.obj instanceof boolean[]) {
                    setAllButton((boolean[]) msg.obj);
                }
            }
        };

        // Set Adapter
        tagIndList = new ArrayList<Integer>();
        for (int i = 0; i < 5; i++)
            tagIndList.add(0);
        myAdapter = new TagListAdapter(getApplicationContext(), tagIndList, SAMPLING_RATE, myHandler);
        tagList.setAdapter(myAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setAllButton(boolean[] enabled) {
        play_record_Btn.setEnabled(enabled[0]);
        stopBtn.setEnabled(enabled[1]);
        readDataBtn.setEnabled(enabled[2]);
    }

    private void setAllButton(boolean enabled) {
        boolean[] sets = { enabled, false, enabled };
        setAllButton(sets);
    }

    private String parseTimeBySec(double secs) {
        if (secs >= 60) {
            int min = (int) (secs / 60);
            double sec = secs - 60 * min;
            String secStr = (sec < 10 ? "0" + (int) sec : "" + (int) sec);
            if (min < 10)
                return "0" + min + ":" + secStr;
            else
                return "" + min + ":" + secStr;
        } else {
            String secStr = (secs < 10 ? "0" + (int) secs : "" + (int) secs);
            return "00:" + secStr;
        }
    }

    private void startRecord() {
        myRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE, CHANNELS, AUDIO_ENCODING, bufferSize);
        myRecorder.startRecording();
        isRecording = true;
        new Thread(new AudioRecordThread()).start();
    }

    private void stopRecord() {
        if (myRecorder != null) {
            isRecording = false;
            myRecorder.stop();
            myRecorder.release();
            myRecorder = null;
        }
    }

    private void startPlay() {
        if (SDCardPath != null) {
            String path = SDCardPath.getAbsolutePath() + "/VoiceBook/" + fileNameEdt.getText().toString();
            path += ".wav";
            try {
                if (mWaveReader != null) {
                    if (!mWaveReader.getFile().getAbsolutePath().equals(path))
                        mWaveReader = new WavReader(path);
                } else {
                    mWaveReader = new WavReader(path);
                }
                myPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                boolean[] sets = {false, true, false};
                new Thread(new loadDataThread(sets)).start();
                isPlaying = true;
                new Thread(new AudioPlayThread()).start();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Toast.makeText(getApplicationContext(), "Something wrong", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void stopPlay() {
        if (myPlayer != null) {
            isPlaying = false;
            myPlayer.stop();
            myPlayer.release();
            myPlayer = null;
            try {
                mWaveReader.getInputStream().close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            setAllButton(true);
        }
    }

    Button.OnClickListener mListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.modeSwitch:
                useMode = !useMode;
                modeSwitch.setText((useMode ? "Recording" : "Playing"));
                play_record_Btn.setText((useMode ? "Record" : "Play"));
                break;
            case R.id.addTag:
                myAdapter.insertTag(useMode ? indRecording : indPlaying);
                Log.d(TAG, "myAdapter.insertTag("
                        + parseTimeBySec((float) (useMode ? indRecording : indPlaying) / SAMPLING_RATE) + ")");
                break;
            case R.id.playBackRateXBtn:
                try {
                    playBackRateX = Float.valueOf(playBackRateXEdt.getText().toString());
                } catch (Exception e) {
                }
                break;
            case R.id.readData:
                try {
                    mWaveReader = new WavReader(SDCardPath.getAbsolutePath() + "/VoiceBook/"
                            + fileNameEdt.getText().toString() + ".wav");
                    boolean[] sets = {true, false, true};
                    new Thread(new loadDataThread(sets, true)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "IOException from class WavReader");
                }
                break;
            case R.id.record_play_btn:
                String fileName = fileNameEdt.getText().toString();
                if (SDCardPath != null && useMode) {
                    Log.d(TAG, "recording: " + fileName + ".wav");
                    v.setEnabled(false);
                    stopBtn.setEnabled(true);
                    startRecord();
                } else {
                    Log.d(TAG, "playing: " + fileName + ".wav");
                    v.setEnabled(false);
                    stopBtn.setEnabled(true);
                    startPlay();
                }
                break;
            case R.id.stopbtn:
                if (useMode) {
                    Log.d(TAG, "stop recording");
                    stopRecord();
                } else {
                    Log.d(TAG, "stop playing");
                    stopPlay();
                }
                v.setEnabled(false);
                play_record_Btn.setEnabled(true);
                break;

            default:
                break;
            }
        }
    };

    class AudioRecordThread implements Runnable {

        @Override
        public void run() {
            String fileName = fileNameEdt.getText().toString();
            if (SDCardPath != null) {
                String filePath = SDCardPath.getAbsolutePath() + "/VoiceBook/" + fileName;
                Log.d(TAG, "writeData2File(" + filePath + ")");
                writeData2File(filePath);
                Log.d(TAG, "copyWaveFile(" + filePath + ", " + filePath + ".wav)");
                WavWriter.copyWaveFile(filePath, filePath + ".wav", SAMPLING_RATE, bufferSize);
            }
        }

    }

    private void writeData2File(String filePath) {
        byte[] audioData = new byte[bufferSize];
        FileOutputStream fos = null;
        int readSize = 0;
        try {
            File file = new File(filePath);
            if (file.exists())
                file.delete();
            fos = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (isRecording) {
            readSize = myRecorder.read(audioData, 0, bufferSize);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                try {
                    fos.write(audioData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            currentTimeBySec += (double) (bufferSize) / SAMPLING_RATE / 2;
            indRecording += bufferSize;
            Message timeMsg = new Message();
            timeMsg.obj = Double.valueOf(currentTimeBySec);
            myHandler.sendMessage(timeMsg);
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentTimeBySec = 0;
        indRecording = 0;
        Message timeMsg = new Message();
        timeMsg.obj = Double.valueOf(currentTimeBySec);
        myHandler.sendMessage(timeMsg);
    }

    class AudioPlayThread implements Runnable {

        @Override
        public void run() {
            while (!mWaveReader.isInitialized())
                ;
            myPlayer.setPlaybackRate((int) mWaveReader.getSampleRate());
            myPlayer.play();
            currentTimeBySec = 0;
            short[] dataToBePlayed = new short[AudioTrack.getMinBufferSize((int) mWaveReader.getSampleRate(),
                    AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING) / 2];
            indPlaying = 0;
            int sampleN = mWaveReader.getSampleNum();
            Log.d(TAG, "Start Playing");
            Message msg = new Message();
            msg.obj = "Stop";
            msg.what = 0;
            myHandler.sendMessage(msg);
            double ratioX;
            try {
                while (isPlaying && indPlaying < sampleN) {
                    ratioX = playBackRateX;
                    dataToBePlayed = mWaveReader.retrieveShortArray(indPlaying, dataToBePlayed.length);
                    if (ratioX >= 1) {
                        myPlayer.write(dataToBePlayed, 0, dataToBePlayed.length);
                        indPlaying += (int) (dataToBePlayed.length / mWaveReader.getChannelsNum() * ratioX);
                    } else {
                        for (int i = (int) (dataToBePlayed.length * ratioX); i < dataToBePlayed.length; i++) {
                            dataToBePlayed[i] = dataToBePlayed[i - (int) (dataToBePlayed.length * ratioX)];
                        }
                        myPlayer.write(dataToBePlayed, 0, dataToBePlayed.length);
                        indPlaying += (int) (dataToBePlayed.length * ratioX);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            isPlaying = false;
            if (indPlaying >= sampleN)
                myPlayer.stop();
            Log.d(TAG, "Stop Playing");
            msg = new Message();
            msg.obj = "Play";
            msg.what = 0;
            myHandler.sendMessage(msg);
            msg = new Message();
            msg.obj = Double.valueOf(0);
            myHandler.sendMessage(msg);
        }
    }

    class loadDataThread implements Runnable {

        private boolean isReadData;
        private boolean[] sets;
        
        public loadDataThread(boolean[] sets, boolean isReadData) {
            this.isReadData = isReadData;
            this.sets = sets;
        }
        
        public loadDataThread(boolean[] sets) {
            this(sets, false);
        }
        
        @Override
        public void run() {
            Message msg;
            if (!mWaveReader.isInitialized()) {
                Log.d(TAG, "Initialing");
                try {
                    msg = new Message();
                    boolean[] sets = { false, false, false };
                    msg.obj = sets;
                    myHandler.sendMessage(msg);
                    mWaveReader.initialize();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            msg = new Message();
            msg.obj = this.sets;
            myHandler.sendMessage(msg);
            mWaveReader.info();
        }
    }

}
