package com.Frank.server;

import java.util.concurrent.*;

public class ThreadPoolManager {
    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 50;
    private static final long KEEP_ALIVE_TIME = 60L;

    // 控制连接线程池（处理命令）
    private static final ExecutorService ctrlThreadPool =
            new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new ThreadFactory() {
                        private int count = 0;
                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "ctrl-pool-" + count++);
                        }
                    });

    // 数据连接线程池（处理文件传输）
    private static final ExecutorService dataThreadPool =
            Executors.newCachedThreadPool(
                    new ThreadFactory() {
                        private int count = 0;
                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "data-pool-" + count++);
                        }
                    });

    public static void executeCtrlTask(Runnable task) {
        ctrlThreadPool.execute(task);
    }

    public static void executeDataTask(Runnable task) {
        dataThreadPool.execute(task);
    }

    public static void shutdown() {
        ctrlThreadPool.shutdown();
        dataThreadPool.shutdown();
        try {
            if (!ctrlThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                ctrlThreadPool.shutdownNow();
            }
            if (!dataThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                dataThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
