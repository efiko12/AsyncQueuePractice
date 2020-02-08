package com.efiko;

import org.apache.commons.lang.mutable.MutableInt;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.JVM)
public class AsyncQueueTests {

    @Test(expected = NullPointerException.class)
    public void nullEnqueue(){
        AsyncQueue<String> queue = getInstance();
        queue.enqueue(null);
    }

    @Test
    public void enqueueAndDequeue() throws Exception {
        AsyncQueue<Integer> queue = getInstance();
        queue.enqueue(1);
        int result = queue.dequeue().get(5, TimeUnit.SECONDS);
        assertEquals(1, result);
    }

    @Test
    public void dequeueAndEnqueue() throws Exception {

        Semaphore waitForDequeue = new Semaphore(0);
        AsyncQueue<Integer> queue = getInstance();
        MutableInt result = new MutableInt(0);

        queue.dequeue().thenAccept(i -> {
            result.setValue(i);
            waitForDequeue.release();
        });

        queue.enqueue(2);
        assertTrue(waitForDequeue.tryAcquire(5, TimeUnit.SECONDS));
        assertEquals(2, result.getValue());

    }

    @Test
    public void mixed() throws Exception {
        Semaphore waitForDequeue1 = new Semaphore(0);
        Semaphore waitForDequeue2 = new Semaphore(0);

        AsyncQueue<String> queue = getInstance();
        StringBuilder result = new StringBuilder();

        queue.dequeue().thenAccept(i1 -> {
            result.append(i1);
            waitForDequeue1.release();


            try {
                String i2 = queue.dequeue().get(5, TimeUnit.SECONDS);
                result.append(i2);
                waitForDequeue2.release();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });

        queue.enqueue("3");
        assertTrue(waitForDequeue1.tryAcquire(5, TimeUnit.SECONDS));
        queue.enqueue("4");
        assertTrue(waitForDequeue2.tryAcquire(5, TimeUnit.SECONDS));
        assertEquals("34", result.toString());

    }

    @Test
    public void stress() throws InterruptedException {
        AsyncQueue<Integer> queue = getInstance();

        Executor executor = Executors.newFixedThreadPool(100);

        IntStream.range(0, 100).forEach(i -> {
            executor.execute(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queue.enqueue(i);
            });
        });

        Set<Integer> set = ConcurrentHashMap.newKeySet();
        Semaphore waitForDequeue = new Semaphore(-99);
        IntStream.range(0, 100).forEach(i -> queue.dequeue().thenAcceptAsync(di -> {
            set.add(di);
            waitForDequeue.release();
        }));

        assertTrue(waitForDequeue.tryAcquire(100, TimeUnit.SECONDS));
        assertEquals(100,set.size());

    }

    private <T> AsyncQueue<T> getInstance() {
        return new AsyncQueue<T>(){

            @Override
            public void enqueue(T element) {

            }

            @Override
            public CompletableFuture<T> dequeue() {
                return null;
            }
        };
    }
}
