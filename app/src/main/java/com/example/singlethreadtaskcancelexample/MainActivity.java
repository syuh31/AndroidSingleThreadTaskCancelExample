package com.example.singlethreadtaskcancelexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.singlethreadtaskcancelexample.StateChangeObject.AsyncStateChangeObject;
import com.example.singlethreadtaskcancelexample.StateChangeObject.CallbackStateChangeObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    class ListViewItem {

        String displayName;
        TaskWithStateMonitoring task;
        List<Future> futures;

        public ListViewItem(String displayName, TaskWithStateMonitoring task){
            this.displayName = displayName;
            this.task = task;
        }

        public void setFutures(List<Future> futures){
            this.futures = futures;
        }

        public void cancel(){
            for(Future future: futures) {
                future.cancel(false);
            }
            taskListViewAdapter.remove(this);
        }

        @NonNull
        @Override
        public String toString() {
            return displayName;
        }
    }

    class AsyncStateChangeObjectTask implements Runnable {
        String displayName;

        public AsyncStateChangeObjectTask(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public void run() {
            asyncStateChangeObject.displayName = displayName;
            asyncStateChangeObject.start();
        }
    }

    class CallbackStateChangeObjectTask implements Runnable {
        String displayName;

        public CallbackStateChangeObjectTask(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public void run() {
            callbackStateChangeObject.displayName = displayName;
            callbackStateChangeObject.start();
        }
    }

    class ChangeActiveListViewTask implements Runnable {

        ListViewItem listViewItem;

        public ChangeActiveListViewTask(ListViewItem listViewItem) {
            this.listViewItem = listViewItem;
        }

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activeItem = listViewItem;
                }
            });
        }
    }

    class RemoveListViewTask implements Runnable {

        ListViewItem listViewItem;

        public RemoveListViewTask(ListViewItem listViewItem) {
            this.listViewItem = listViewItem;
        }

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    taskListViewAdapter.remove(listViewItem);
                    activeItem = null;
                }
            });
        }
    }

    ListView taskListView;
    ArrayAdapter taskListViewAdapter;
    Button submitAsyncBtn;
    Button submitCallbackBtn;

    ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
    Handler handler;
    AsyncStateChangeObject asyncStateChangeObject = new AsyncStateChangeObject();
    CallbackStateChangeObject callbackStateChangeObject = new CallbackStateChangeObject();
    ListViewItem activeItem;
    int submitTaskCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());
        taskListView = findViewById(R.id.taskListView);
        submitAsyncBtn = findViewById(R.id.submitAsyncStateChangeTaskBtn);
        submitCallbackBtn = findViewById(R.id.submitCallbackStateChangeTaskBtn);

        taskListViewAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, new ArrayList());
        taskListView.setAdapter(taskListViewAdapter);

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListViewItem targetItem = (ListViewItem) taskListView.getItemAtPosition(position);
                if (targetItem == activeItem) {
                    targetItem.task.cancel();
                } else {
                    targetItem.cancel();
                }
            }
        });

        submitAsyncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAsyncStateTask();
            }
        });

        submitCallbackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCallbackStateTask();
            }
        });

        callbackStateChangeObject.setOnStopListener(new CallbackStateChangeObject.OnStopListener() {
            @Override
            public void onStop() {

            }
        });
    }

    void submitAsyncStateTask(){

        TaskWithStateMonitoring.Supplier<Boolean> isReady = new TaskWithStateMonitoring.Supplier<Boolean>() {
            @Override
            public Boolean get() {
                AsyncStateChangeObject.State state = asyncStateChangeObject.state.get();

                return state == AsyncStateChangeObject.State.stop
                        || state == AsyncStateChangeObject.State.none;
            }
        };

        Runnable task = new AsyncStateChangeObjectTask("async state task(" + submitTaskCount + ")");

        Runnable cancel = new Runnable() {
            @Override
            public void run() {
                asyncStateChangeObject.stop();
            }
        };

        CancelableTask cancelableTask = new CancelableTask(task, cancel);

        TaskWithStateMonitoring.Supplier<Boolean> isEnd = new TaskWithStateMonitoring.Supplier<Boolean>() {
            @Override
            public Boolean get() {
                AsyncStateChangeObject.State state = asyncStateChangeObject.state.get();
                return state == AsyncStateChangeObject.State.stop;
            }
        };

        TaskWithStateMonitoring taskWithStateMonitoring = new TaskWithStateMonitoring(isReady, cancelableTask, isEnd);

        ListViewItem listViewItem = new ListViewItem("asyncStateTask" + submitTaskCount, taskWithStateMonitoring);
        taskListViewAdapter.add(listViewItem);
        Future changeActiveItemFuture = taskExecutor.submit(new ChangeActiveListViewTask(listViewItem));
        Future taskFuture = taskExecutor.submit(taskWithStateMonitoring);
        submitTaskCount++;
        Future removeListFuture = taskExecutor.submit(new RemoveListViewTask(listViewItem));
        listViewItem.setFutures(Arrays.asList(changeActiveItemFuture, taskFuture, removeListFuture));
    }

    void submitCallbackStateTask(){

        TaskWithStateMonitoring.Supplier<Boolean> isReady = new TaskWithStateMonitoring.Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return !callbackStateChangeObject.isPlaying;
            }
        };

        Runnable task = new CallbackStateChangeObjectTask("callback state task(" + submitTaskCount + ")");

        Runnable cancel = new Runnable() {
            @Override
            public void run() {
                callbackStateChangeObject.stop();
            }
        };

        CancelableTask cancelableTask = new CancelableTask(task, cancel);

        TaskWithStateMonitoring.Supplier<Boolean> isEnd = new TaskWithStateMonitoring.Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return !callbackStateChangeObject.isPlaying;
            }
        };

        TaskWithStateMonitoring taskWithStateMonitoring = new TaskWithStateMonitoring(isReady, cancelableTask, isEnd);

        ListViewItem listViewItem = new ListViewItem("callbackStateTask" + submitTaskCount, taskWithStateMonitoring);
        taskListViewAdapter.add(listViewItem);
        Future changeActiveItemFuture = taskExecutor.submit(new ChangeActiveListViewTask(listViewItem));
        Future taskFuture = taskExecutor.submit(taskWithStateMonitoring);
        submitTaskCount++;
        Future removeListFuture = taskExecutor.submit(new RemoveListViewTask(listViewItem));
        listViewItem.setFutures(Arrays.asList(changeActiveItemFuture, taskFuture, removeListFuture));
    }
}
