package com.qmz.canteen.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CertificateRequest {

    @Size(max = 255, message = "健康证地址不合法")
    private String healthCertUrl;

    @Size(max = 255, message = "营业执照地址不合法")
    private String licenseUrl;
}
