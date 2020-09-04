package com.alishangtian.mtimer.common;

/**
 * @Desc RemotingCommandResultEnums
 * @Time 2020/08/30
 * @Author alishangtian
 */
public enum RemotingCommandResultEnums {
    /**
     * 成功
     */
    SUCCESS(1, "成功"),
    /**
     * 失败
     */
    FAILED(0, "失败");
    private int result;
    private String desc;

    RemotingCommandResultEnums(int result, String desc) {
        this.result = result;
        this.desc = desc;
    }

    public int getResult() {
        return result;
    }

    public String getDesc() {
        return desc;
    }

}
