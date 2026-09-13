package com.ruoyi.framework.config;

import com.github.binarywang.wxpay.v3.auth.Verifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 微信支付公钥模式验签器。
 *
 * <p>2023 年之后新入驻的微信支付商户默认使用"微信支付公钥"模式：
 * 请求签名仍使用商户 API 私钥，但响应与回调验签使用商户平台下载的
 * 微信支付公钥（pub_key.pem），响应头 Wechatpay-Serial 为公钥ID。
 * 旧版 SDK 的自动平台证书下载器（AutoUpdateCertificatesVerifier）
 * 在此模式下会得到 404（无可用的平台证书），因此需要注入本验签器。</p>
 */
@Slf4j
public class WechatPayPublicKeyVerifier implements Verifier {

    private final String publicKeyId;
    private final PublicKey publicKey;
    private final X509Certificate dummyCertificate;

    public WechatPayPublicKeyVerifier(String publicKeyId, String publicKeyPath) {
        this.publicKeyId = publicKeyId;
        this.publicKey = parsePublicKey(loadPublicKeyPem(publicKeyPath));
        this.dummyCertificate = new DummyX509Certificate(buildSerial(publicKeyId));
    }

    @Override
    public boolean verify(String serialNumber, byte[] message, String signature) {
        if (!publicKeyId.equals(serialNumber)) {
            log.warn("微信支付公钥ID不匹配, 期望: {}, 实际: {}", publicKeyId, serialNumber);
            return false;
        }
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initVerify(publicKey);
            signer.update(message);
            return signer.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.warn("微信支付公钥验签失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public X509Certificate getValidCertificate() {
        return dummyCertificate;
    }

    private static String loadPublicKeyPem(String path) {
        try {
            if (path.startsWith("classpath:")) {
                ClassPathResource resource = new ClassPathResource(path.substring("classpath:".length()));
                try (InputStream in = resource.getInputStream()) {
                    return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
                }
            }
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("读取微信支付公钥文件失败: " + path, e);
        }
    }

    private static PublicKey parsePublicKey(String pem) {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("解析微信支付公钥失败", e);
        }
    }

    private static BigInteger buildSerial(String publicKeyId) {
        try {
            return new BigInteger(1, MessageDigest.getInstance("SHA-256")
                    .digest(publicKeyId.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return BigInteger.ONE;
        }
    }

    /**
     * 仅用于向 SDK 提供请求头 Wechatpay-Serial 所需的证书序列号，
     * 序列号由公钥ID派生，其余方法均返回默认值。
     */
    private static class DummyX509Certificate extends X509Certificate {

        private final BigInteger serialNumber;

        DummyX509Certificate(BigInteger serialNumber) {
            this.serialNumber = serialNumber;
        }

        @Override
        public BigInteger getSerialNumber() {
            return serialNumber;
        }

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public Date getNotBefore() {
            return new Date(0);
        }

        @Override
        public Date getNotAfter() {
            return new Date(Long.MAX_VALUE);
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        }

        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey key) {
        }

        @Override
        public void verify(PublicKey key, String sigProvider) {
        }

        @Override
        public String toString() {
            return "WechatPayPublicKey:" + serialNumber;
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return null;
        }

        @Override
        public java.security.Principal getIssuerDN() {
            return null;
        }

        @Override
        public java.security.Principal getSubjectDN() {
            return null;
        }

        @Override
        public byte[] getTBSCertificate() {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return "SHA256withRSA";
        }

        @Override
        public String getSigAlgOID() {
            return "1.2.840.113549.1.1.11";
        }

        @Override
        public byte[] getSigAlgParams() {
            return null;
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return null;
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }

        @Override
        public boolean[] getKeyUsage() {
            return null;
        }

        @Override
        public List<String> getExtendedKeyUsage() {
            return null;
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }
    }
}