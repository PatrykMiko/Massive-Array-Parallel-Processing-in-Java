package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Demonstrates modern asynchronous computation of an array's mean using CompletableFuture.
 * Shows two different approaches to aggregating asynchronous results.
 */
public class AsyncMean {
    static double[] array;

    static void initArray(int size) {
        array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = Math.random() * size / (i + 1);
        }
    }

    public static void main(String[] args) {
        asyncMeanv1();
        asyncMeanv2();
    }

    /**
     * A Supplier task that computes the mean of an array chunk and returns the result.
     * Used in conjunction with CompletableFuture.
     */
    static class MeanCalcSupplier implements Supplier<Double> {
        private final int start;
        private final int end;

        MeanCalcSupplier(int start, int end){
            this.start = start;
            this.end = end;
        }

        @Override
        public Double get() {
            double sum = 0;
            double mean = 0;
            for (int i = start; i < end; i++){
                sum += array[i];
            }
            if(end - start > 0) {
                mean = sum / (end - start);
            }
            return mean;
        }
    }

    /**
     * Approach 1: Uses a List of CompletableFutures and the join() method
     * to wait for all asynchronous computations to finish before aggregating.
     */
    public static void asyncMeanv1() {
        int size = 100_000_000;
        initArray(size);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        int n = 64; // Number of chunks
        int blockSize = size / n;

        double t1 = System.nanoTime() / 1e6;
        List<CompletableFuture<Double>> partialResults = new ArrayList<>();

        for(int i = 0; i < n; i++){
            int start = i * blockSize;
            int end = start + blockSize;
            if (i == n - 1) end = size;

            // Dispatch async task
            CompletableFuture<Double> partialMean = CompletableFuture.supplyAsync(
                    new MeanCalcSupplier(start, end), executor);
            partialResults.add(partialMean);
        }

        double sumMean = 0;
        // join() blocks the current thread until the specific CompletableFuture completes
        for(var pr : partialResults){
            sumMean += pr.join();
        }

        double mean = sumMean / n;
        double t2 = System.nanoTime() / 1e6;
        System.out.printf(Locale.US, "v1 (join) -> t2-t1=%f ms mean=%f\n", t2 - t1, mean);

        executor.shutdown();
    }

    /**
     * Approach 2: Uses CompletableFuture.thenApply() to form a reactive pipeline.
     * As soon as a computation finishes, it pushes its result to a BlockingQueue.
     */
    static void asyncMeanv2() {
        int size = 100_000_000;
        initArray(size);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        int n = 64;
        int blockSize = size / n;

        // Queue size matches chunk count 'n'
        BlockingQueue<Double> queue = new ArrayBlockingQueue<>(n);

        double t1 = System.nanoTime() / 1e6;
        for (int i = 0; i < n; i++) {
            int start = i * blockSize;
            int end = start + blockSize;
            if (i == n - 1) end = size;

            // Dispatch async task and pipe the result directly into the queue upon completion
            CompletableFuture.supplyAsync(new MeanCalcSupplier(start, end), executor)
                    .thenApply(d -> queue.offer(d));
        }

        double sumMean = 0;
        try {
            // Wait for and aggregate results exactly 'n' times
            for (int i = 0; i < n; i++) {
                sumMean += queue.take(); // Blocks if queue is empty
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        double mean = sumMean / n;
        double t2 = System.nanoTime() / 1e6;

        System.out.printf(Locale.US, "v2 (thenApply) -> t2-t1=%f ms mean=%f\n", t2 - t1, mean);

        executor.shutdown();
    }
}