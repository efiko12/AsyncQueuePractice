package com.efiko;

import java.util.concurrent.CompletableFuture;

public interface AsyncQueue<T> {

    /**
     * add the specified element into this queue
     *
     * @param element to insert
     * @throws NullPointerException if the specified element is null and
     *                              this queue does not permit null elements
     */
    void enqueue(T element);

    /**
     * Returns a CompletableFuture which will be completed when an element will be available.
     * The order of the the elements will be correct in case of a single dequeue process.
     * In case of parallel calls, the order of CompletableFuture completion is arbitrary.
     * @return CompletableFuture
     */
    CompletableFuture<T> dequeue();


}
