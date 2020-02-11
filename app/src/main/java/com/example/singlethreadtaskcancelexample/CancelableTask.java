package com.example.singlethreadtaskcancelexample;

public class CancelableTask implements Runnable {

    Runnable task;
    Runnable cancel;

    public CancelableTask(Runnable task, Runnable cancel){
        this.task = task;
        this.cancel = cancel;
    }

    @Override
    public void run() {
        task.run();
    }

    public void cancel() {
        cancel.run();
    }
}
