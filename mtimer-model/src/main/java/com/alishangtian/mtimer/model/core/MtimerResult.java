package com.alishangtian.mtimer.model.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc MtimerResult
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MtimerResult {
    private String partition;
    @Builder.Default
    private boolean success = false;
    private String msg;
}
