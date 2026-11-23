package com.yunzhongyi.medical.newtech.dto;

import lombok.Data;

/**
 * 伦理审查请求：是否通过 + 审查人 + 审查意见
 */
@Data
public class NewTechEthicsRequest {

    /** 是否通过伦理审查 */
    private Boolean passed;

    /** 伦理审查人 */
    private String reviewer;

    /** 伦理审查意见 */
    private String opinion;
}
