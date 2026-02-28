package org.example;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstrates parallel computation of an array's mean using a Thread Pool
 * and a BlockingQueue to collect partial results.
 */
public class Mean {
    static double[] array;
    // Thread-safe queue to store the results calculated by individual threads
    static BlockingQueue<Double> results = new ArrayBlockingQueue<Double>(150);

    /**
     * Initializes a massive array with pseudo-random double values.
     * @param size The number of elements in the array.
     */
    static void initArray(int size){
        array = new double[size];
        for(int i = 0; i < size; i++){
            array[i] = Math.random() * size / (i + 1);
        }
    }

    public static void main(String[] args) {
        // Initialize an array with 128 million elements
        initArray(128_000_000);

        // Benchmark the computation across different numbers of tasks (chunks)
        for(int cnt : new int[]{1, 2, 4, 8, 16, 32, 64, 128}){
            parallelMean(cnt);
        }
    }

    /**
     * A Runnable task that calculates the mean of a specific chunk of the array.
     */
    static class MeanCalc implements Runnable {
        private final int start;
        private final int end;
        double mean = 0;

        MeanCalc(int start, int end){
            this.start = start;
            this.end = end;
        }

        public void run(){
            double sum = 0;
            for (int i = start; i < end; i++){
                sum += array[i];
            }
            if(end - start > 0) {
                mean = sum / (end - start);
            }
            try {
                // Safely put the calculated partial mean into the blocking queue
                results.put(mean);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Divides the array into 'cnt' chunks and processes them concurrently
     * using an ExecutorService.
     * * @param cnt The number of chunks to divide the array into.
     */
    static void parallelMean(int cnt){
        results.clear();
        // Create a thread pool with 16 fixed threads (ideal for modern multi-core CPUs)
        ExecutorService executor = Executors.newFixedThreadPool(16);
        int blockSize = array.length / cnt;

        double t1 = System.nanoTime() / 1e6;

        // Dispatch tasks to the executor
        for (int i = 0; i < cnt; i++) {
            int start = i * blockSize;
            int end = start + blockSize;
            // Ensure the last chunk covers the rest of the array if not perfectly divisible
            if (i == cnt - 1) end = array.length;
            executor.execute(new MeanCalc(start, end));
        }

        executor.shutdown();
        double t2 = System.nanoTime() / 1e6;

        double sumMean = 0;
        try {
            // Retrieve partial means from the queue. take() blocks if empty.
            for (int i = 0; i < cnt; i++) {
                double partialMean = results.take();
                sumMean += partialMean;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        double mean = sumMean / cnt;
        double t3 = System.nanoTime() / 1e6;

        System.out.printf(Locale.US, "size = %d cnt=%d >  t2-t1=%f ms t3-t1=%f ms mean=%f\n",
                array.length,
                cnt,
                t2 - t1, // Task dispatch time
                t3 - t1, // Total execution time
                mean);
    }
}