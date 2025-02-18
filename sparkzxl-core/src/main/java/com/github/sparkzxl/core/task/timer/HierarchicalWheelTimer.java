package com.github.sparkzxl.core.task.timer;

import cn.hutool.core.thread.NamedThreadFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * description: HierarchicalWheelTimer
 *
 * <p>The type Hierarchical Wheel timer.</p>
 *
 * @author zhouxinlei
 * @see TimingWheel
 * @since 2022-08-25 13:36:14
 */
public class HierarchicalWheelTimer implements Timer {

    private static final AtomicIntegerFieldUpdater<HierarchicalWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(HierarchicalWheelTimer.class, "workerState");

    private final ExecutorService taskExecutor;

    private final DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();

    private final AtomicInteger taskCounter = new AtomicInteger(0);

    private final TimingWheel timingWheel;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private final Thread workerThread;
    private volatile int workerState;

    /**
     * Instantiates a new System timer.
     *
     * @param executorName the executor name
     */
    public HierarchicalWheelTimer(final String executorName) {
        this(executorName, 1L, 20, TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
    }

    /**
     * Instantiates a new System timer.
     *
     * @param executorName the executor name
     * @param tickMs       the tick ms
     * @param wheelSize    the wheel size
     * @param startMs      the start ms
     */
    public HierarchicalWheelTimer(final String executorName,
            final Long tickMs,
            final Integer wheelSize,
            final Long startMs) {
        ThreadFactory threadFactory = new NamedThreadFactory(executorName, false);
        taskExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
        workerThread = threadFactory.newThread(new Worker(this));
        timingWheel = new TimingWheel(tickMs, wheelSize, startMs, taskCounter, delayQueue);
    }

    @Override
    public void add(final TimerTask timerTask) {
        if (timerTask == null) {
            throw new NullPointerException("timer task null");
        }
        this.readLock.lock();
        try {
            start();
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            this.addTimerTaskEntry(new TimerTaskList.TimerTaskEntry(this, timerTask, timerTask.getDelayMs() + millis));
        } finally {
            this.readLock.unlock();
        }

    }

    private void addTimerTaskEntry(final TimerTaskList.TimerTaskEntry timerTaskEntry) {
        if (!timingWheel.add(timerTaskEntry)) {
            if (!timerTaskEntry.cancelled()) {
                taskExecutor.submit(() -> timerTaskEntry.getTimerTask().run(timerTaskEntry));
            }
        }
    }

    @Override
    public void advanceClock(final long timeoutMs) throws InterruptedException {
        TimerTaskList bucket = delayQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (bucket != null) {
            writeLock.lock();
            try {
                while (bucket != null) {
                    timingWheel.advanceClock(bucket.getExpiration());
                    bucket.flush(this::addTimerTaskEntry);
                    bucket = delayQueue.poll();
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    private void start() {
        int state = WORKER_STATE_UPDATER.get(this);
        if (state == 0) {
            if (WORKER_STATE_UPDATER.compareAndSet(this, 0, 1)) {
                workerThread.start();
            }
        }
    }

    @Override
    public int size() {
        return taskCounter.get();
    }

    @Override
    public void shutdown() {
        taskExecutor.shutdown();
    }

    private static class Worker implements Runnable {

        private final Timer timer;

        /**
         * Instantiates a new Worker.
         *
         * @param timer the timer
         */
        Worker(final Timer timer) {
            this.timer = timer;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    timer.advanceClock(100L);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
