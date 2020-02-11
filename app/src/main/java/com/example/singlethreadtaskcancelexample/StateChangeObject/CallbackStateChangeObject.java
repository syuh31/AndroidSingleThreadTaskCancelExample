package com.example.singlethreadtaskcancelexample.StateChangeObject;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CallbackStateChangeObject {

    static final Executor executer = Executors.newSingleThreadExecutor();

    public String displayName = "";
    AtomicBoolean terminate = new AtomicBoolean(false);
    Handler handler;
    OnStopListener onStopListener;
    public boolean isPlaying;

    public CallbackStateChangeObject(){
        handler = new Handler(Looper.myLooper());
    }

    public interface OnStopListener{
        void onStop();
    }

    public void setOnStopListener(OnStopListener listener) {
        this.onStopListener = listener;
    }

    public void start() {
        terminate.set(false);
        isPlaying = true;
        executer.execute(new Runnable() {
            @Override
            public void run() {

                int count = 0;
                while (count < 50) {
                    if (terminate.get()) {
                        break;
                    }

                    try {
                        Thread.sleep(100);
                        System.out.println(displayName + " : working!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    count++;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        isPlaying = false;
                        onStopListener.onStop();
                    }
                });
            }
        });
    }

    public void stop() {
        terminate.set(true);
        System.out.println("cancel");
    }
}
