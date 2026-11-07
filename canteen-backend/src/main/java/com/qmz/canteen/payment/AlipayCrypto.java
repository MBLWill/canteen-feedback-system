package com.qmz.canteen.payment;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** 支付宝 RSA2 签名与验签，不依赖官方 SDK */
public final class AlipayCrypto {

    private static final String ALGORITHM = "SHA256withRSA";

    private AlipayCrypto() {
    }

    public static PrivateKey privateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(strip(base64));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("应用私钥解析失败，请确认是 PKCS8 格式", e);
        }
    }

    public static PublicKey publicKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(strip(base64));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("支付宝公钥解析失败，请确认是 X.509 格式", e);
        }
    }

    /** 请求签名：排除 sign，其余非空参数参与（sign_type 要计入） */
    public static String sign(Map<String, String> params, PrivateKey key) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(key);
            signature.update(canonical(params, true).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败：" + e.getMessage(), e);
        }
    }

    /** 回调验签：sign 与 sign_type 都不参与（支付宝的规定） */
    public static boolean verify(Map<String, String> params, PublicKey key) {
        String sign = params.get("sign");
        if (sign == null || sign.isBlank()) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(key);
            signature.update(canonical(params, false).getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    public static String canonical(Map<String, String> params, boolean withSignType) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !"sign".equals(e.getKey()))
                .filter(e -> withSignType || !"sign_type".equals(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String strip(String key) {
        return key.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    }
}
