package com.goodlife.voicebook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class TagListAdapter extends BaseAdapter {

    private ArrayList<Integer> tagIndList;
    private int sampleRate;
    private LayoutInflater inflater;
    private Handler handler;

    public TagListAdapter(Context context, ArrayList<Integer> tagIndList, int sampleRate, Handler handler) {
        this.tagIndList = tagIndList;
        this.sampleRate = sampleRate;
        this.inflater = LayoutInflater.from(context);
        this.handler = handler;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return tagIndList.size();
    }

    @Override
    public Object getItem(int pos) {
        // TODO Auto-generated method stub
        return tagIndList.get(pos);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewTag tag = new ViewTag();
        final int ind = position;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.taglistadapter, null);
            tag.timeTag = (TextView) convertView.findViewById(R.id.tagTime);
            tag.deleteTag = (Button) convertView.findViewById(R.id.deleteTag);
            tag.jumpTag = (Button) convertView.findViewById(R.id.jumpTag);
            convertView.setTag(tag);
        } else {
            tag = (ViewTag) convertView.getTag();
        }
        tag.timeTag.setText("Tag" + position + " : "
                + parseTimeBySec((float) tagIndList.get(position) / (float) sampleRate));
        tag.deleteTag.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (tagIndList.get(ind) > 0) {
                    tagIndList.remove(ind);
                    if (tagIndList.size() < 5)
                        tagIndList.add(0);
                    notifyDataSetChanged();
                    Log.d("VoiceBook", "remove " + ind);
                }
                if (tagIndList.size() < 5) {
                    insertTag(0);
                }
            }

        });
        tag.jumpTag.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Message msg = new Message();
                msg.obj = "Jump";
                msg.what = tagIndList.get(ind);
                handler.sendMessage(msg);
            }

        });

        return convertView;
    }

    class ViewTag {
        public TextView timeTag;
        public Button jumpTag;
        public Button deleteTag;
    }

    private String parseTimeBySec(float secs) {
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

    public void insertTag(int ind) {
        for (int i = 0; i < tagIndList.size(); i++) {
            if (ind >= tagIndList.get(i)) {
                tagIndList.add(i, Integer.valueOf(ind));
                if (tagIndList.get(tagIndList.size() - 1) == 0)
                    tagIndList.remove(tagIndList.size() - 1);
                notifyDataSetChanged();
                return;
            }
        }
        tagIndList.add(Integer.valueOf(ind));
        notifyDataSetChanged();
    }

}
