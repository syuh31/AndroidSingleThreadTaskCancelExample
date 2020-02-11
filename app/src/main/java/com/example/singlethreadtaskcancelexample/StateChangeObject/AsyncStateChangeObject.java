package com.example.singlethreadtaskcancelexample.StateChangeObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncStateChangeObject {

    static final Executor executer = Executors.newSingleThreadExecutor();

    public enum State {
        none,
        start,
        stop,
    }

    public String displayName = "";
    public AtomicReference<State> state = new AtomicReference<>(State.none);
    AtomicBoolean terminate = new AtomicBoolean(false);

    public void start() {
        terminate.set(false);
        state.set(State.start);
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

                state.set(State.stop);
            }
        });
    }

    public void stop() {
        terminate.set(true);
        System.out.println("cancel");
    }
}
