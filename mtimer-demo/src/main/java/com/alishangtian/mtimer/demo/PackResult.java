package com.alishangtian.mtimer.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc PackResult
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackResult {
    private int code;
    private String msg;
    private Object data;
}
