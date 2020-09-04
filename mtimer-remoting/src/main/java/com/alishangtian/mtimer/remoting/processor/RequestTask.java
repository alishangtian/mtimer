package com.alishangtian.mtimer.remoting.processor;

import com.alishangtian.mtimer.remoting.MtimerCommand;
import io.netty.channel.Channel;

/**
 * @Desc RequestTask
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class RequestTask implements Runnable {
    private final Runnable runnable;
    private final long createTimestamp = System.currentTimeMillis();
    private final Channel channel;
    private final MtimerCommand request;
    private boolean stopRun = false;

    public RequestTask(final Runnable runnable, final Channel channel, final MtimerCommand request) {
        this.runnable = runnable;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public int hashCode() {
        int result = runnable != null ? runnable.hashCode() : 0;
        result = 31 * result + (int) (getCreateTimestamp() ^ (getCreateTimestamp() >>> 32));
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (request != null ? request.hashCode() : 0);
        result = 31 * result + (isStopRun() ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RequestTask)) {
            return false;
        }

        final RequestTask that = (RequestTask) o;

        if (getCreateTimestamp() != that.getCreateTimestamp()) {
            return false;
        }
        if (isStopRun() != that.isStopRun()) {
            return false;
        }
        if (channel != null ? !channel.equals(that.channel) : that.channel != null) {
            return false;
        }
        return request != null ? request.getOpaque() == that.request.getOpaque() : that.request == null;

    }

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public boolean isStopRun() {
        return stopRun;
    }

    public void setStopRun(final boolean stopRun) {
        this.stopRun = stopRun;
    }

    @Override
    public void run() {
        if (!this.stopRun) {
            this.runnable.run();
        }
    }

    public void returnResponse(int code, String remark) {
        final MtimerCommand response = MtimerCommand.builder().remark(remark).code(code).opaque(request.getOpaque()).build();
        this.channel.writeAndFlush(response);
    }
}
