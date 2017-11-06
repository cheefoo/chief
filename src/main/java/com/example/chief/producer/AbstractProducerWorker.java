package com.example.chief.producer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractProducerWorker implements Runnable {

    protected final BlockingQueue<Object> inputQueue;
    protected volatile boolean shutdown = false;
    protected final AtomicLong recordsPut = new AtomicLong(0);
    protected final ObjectMapper mapper = new ObjectMapper();
    
    protected AbstractProducerWorker(
            BlockingQueue<Object> inputQueue) {
        this.inputQueue = inputQueue;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                runOnce();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public long recordsPut() {
        return recordsPut.get();
    }

    public void stop() {
        shutdown = true;
    }

    protected abstract void runOnce() throws Exception;
}