package com.example.singlethreadtaskcancelexample;

import java.util.concurrent.atomic.AtomicBoolean;

public class TaskWithStateMonitoring implements Runnable {

    public interface Supplier<T> {
        T get();
    }

    AtomicBoolean isRunning = new AtomicBoolean(false);
    CancelableTask task;
    Supplier<Boolean> isReady;
    Supplier<Boolean> isEnd;

    public TaskWithStateMonitoring(Supplier<Boolean> isReady, CancelableTask task, Supplier<Boolean> isEnd) {
        this.isReady = isReady;
        this.task = task;
        this.isEnd = isEnd;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void run() {
        System.out.println("start task");

        while (!isReady.get()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        task.run();

        while (!isEnd.get()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("end task");
    }

    public void cancel(){
        task.cancel();
    }
}
