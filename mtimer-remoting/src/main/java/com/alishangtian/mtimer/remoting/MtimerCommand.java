package com.alishangtian.mtimer.remoting;

import com.alishangtian.mtimer.common.RemotingCommandResultEnums;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.remoting.common.MtimerCommandType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Desc MtimerCommand
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
public class MtimerCommand implements Serializable {
    /**
     * two bit low bit mark request/response high bit isoneway or not
     */
    private int flag;
    private int code;
    private String remark;
    private byte[] load;
    private static AtomicLong requestId = new AtomicLong(0);
    @lombok.Builder.Default
    private long opaque = requestId.getAndIncrement();
    private int result;
    private String hostAddr;
    @lombok.Builder.Default
    private boolean waitingFollowerTopology = false;

    public ByteBuffer encode() {
        byte[] bytes = encodeBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        return byteBuffer;
    }

    public byte[] encodeBytes() {
        return JSONUtils.toJSONString(this).getBytes(JSONUtils.CHARSET_UTF8);
    }

    public static MtimerCommand decode(final byte[] array) {
        return JSONUtils.parseObject(array, MtimerCommand.class);
    }

    public static MtimerCommand decode(final ByteBuffer byteBuffer) {
        return JSONUtils.parseObject(byteBuffer.array(), MtimerCommand.class);
    }

    public void markOnewayRPC() {
        int bits = 1 << 1;
        this.flag |= bits;
    }

    @JsonIgnore
    public boolean isOnewayRPC() {
        int bits = 1 << 1;
        return (this.flag & bits) == bits;
    }

    @JsonIgnore
    public MtimerCommandType getType() {
        if (this.isResponseType()) {
            return MtimerCommandType.RESPONSE_COMMAND;
        }

        return MtimerCommandType.REQUEST_COMMAND;
    }

    @JsonIgnore
    public boolean isResponseType() {
        int bits = 1 << 0;
        return (this.flag & bits) == bits;
    }

    public MtimerCommand markResponseType() {
        int bits = 1 << 0;
        this.flag |= bits;
        return this;
    }

    public boolean isSuccess() {
        return this.result == RemotingCommandResultEnums.SUCCESS.getResult();
    }

}
