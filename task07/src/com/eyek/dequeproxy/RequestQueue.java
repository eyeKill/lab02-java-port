package com.eyek.dequeproxy;

import java.nio.channels.SocketChannel;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

class RequestElem {
    private SocketChannel channel;
    private Date timeReceived;

    public RequestElem(SocketChannel socketChannel, Date timeReceived) {
        this.channel = socketChannel;
        this.timeReceived = timeReceived;
    }

    public RequestElem(SocketChannel socketChannel) {
        this(socketChannel, new Date());
    }

    public Date getTimeReceived() {
        return timeReceived;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}

public class RequestQueue {

    public static long defaultTimeoutMillis = 2500; // 2.5s
    // act like a stack if more than 20 requests are in the queue
    public static int defaultThreshold = 10;

    private final BlockingDeque<RequestElem> queue;
    private final long timeoutMillis;
    private final long threshold;

    public RequestQueue(long timeoutMillis, int threshold) {
        this.queue = new LinkedBlockingDeque<>(Integer.MAX_VALUE);
        this.timeoutMillis = timeoutMillis;
        this.threshold = threshold;
        if(this.timeoutMillis <= 0)
            throw new InvalidParameterException(String.format("Timeout should be positive, %d given.", timeoutMillis));
        if(this.threshold <= 0)
            throw new InvalidParameterException(String.format("Threshold should be positive, %d given.", threshold));
    }

    public RequestQueue() {// act like a stack if more than 50 requests are in the queue
        this(defaultTimeoutMillis, defaultThreshold);
    }

    // remove outdated requests
    public int refresh() {
        synchronized (queue) {
            if (queue.isEmpty())
                return 0;

            long timeNow = System.currentTimeMillis();
            int ret = 0;
            try {
                System.out.println(timeNow - queue.getFirst().getTimeReceived().getTime());
                while (timeNow - queue.getFirst().getTimeReceived().getTime() > this.timeoutMillis) {
                    ret++;
                    queue.pollFirst().getChannel().close();
                    if (queue.poll() == null)
                        // empty
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret;
        }
    }

    public void addOne(RequestElem e) {
        queue.offer(e);
        System.out.println(String.format("Received one, size = %d.", getSize()));
    }

    // get one element according to the stack/queue role
    // block if no element is present
    public RequestElem getOne() {
        int remove = this.refresh();
        if(remove > 0)
            System.out.println(String.format("%d request(s) ignored", remove));
        try {
            if (getSize() > threshold) {
                System.out.println("Getting as stack");
                return queue.takeLast();
            }
            else {
                System.out.println("Getting as queue");
                return queue.takeFirst();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getSize() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
