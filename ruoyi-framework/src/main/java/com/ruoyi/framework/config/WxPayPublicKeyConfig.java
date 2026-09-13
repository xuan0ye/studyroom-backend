package com.ruoyi.framework.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.config.WxPayHttpProxy;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.util.HttpProxyUtils;
import com.github.binarywang.wxpay.v3.WxPayV3HttpClientBuilder;
import com.github.binarywang.wxpay.v3.auth.WxPayValidator;
import com.github.binarywang.wxpay.v3.util.PemUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * 微信支付公钥模式配置。
 *
 * <p>weixin-java-pay 4.3.0 的 {@code WxPayConfig.initApiV3HttpClient()} 会强制创建
 * {@code AutoUpdateCertificatesVerifier} 并覆盖 verifier 字段。新入驻商户使用
 * "微信支付公钥"模式，平台没有可下载的证书（返回 404），导致验签时 NPE。
 * 本类重写该方法，使用 {@link WechatPayPublicKeyVerifier} 完成响应验签。</p>
 */
@Slf4j
public class WxPayPublicKeyConfig extends WxPayConfig {

    private final String publicKeyId;
    private final String publicKeyPath;

    public WxPayPublicKeyConfig(String publicKeyId, String publicKeyPath) {
        this.publicKeyId = publicKeyId;
        this.publicKeyPath = publicKeyPath;
    }

    @Override
    public CloseableHttpClient initApiV3HttpClient() throws WxPayException {
        try (InputStream keyStream = loadConfigInputStream(getPrivateKeyPath(), "privateKeyPath");
             InputStream certStream = loadConfigInputStream(getPrivateCertPath(), "privateCertPath")) {
            PrivateKey privateKey = PemUtils.loadPrivateKey(keyStream);
            X509Certificate merchantCert = PemUtils.loadCertificate(certStream);
            if (StringUtils.isBlank(getCertSerialNo())) {
                setCertSerialNo(merchantCert.getSerialNumber().toString(16).toUpperCase());
            }
            WechatPayPublicKeyVerifier verifier = new WechatPayPublicKeyVerifier(publicKeyId, publicKeyPath);
            WxPayV3HttpClientBuilder builder = WxPayV3HttpClientBuilder.create()
                .withMerchant(getMchId(), getCertSerialNo(), privateKey)
                .withWechatpay(Collections.singletonList(merchantCert))
                .withValidator(new WxPayValidator(verifier));
            HttpProxyUtils.initHttpProxy(builder, buildHttpProxy());
            CloseableHttpClient httpClient = builder.build();
            setApiV3HttpClient(httpClient);
            setVerifier(verifier);
            setPrivateKey(privateKey);
            return httpClient;
        } catch (WxPayException e) {
            throw e;
        } catch (Exception e) {
            throw new WxPayException("v3初始化异常", e);
        }
    }

    private WxPayHttpProxy buildHttpProxy() {
        if (StringUtils.isNotBlank(getHttpProxyHost())
            && getHttpProxyPort() != null && getHttpProxyPort() > 0) {
            return new WxPayHttpProxy(getHttpProxyHost(), getHttpProxyPort(),
                getHttpProxyUsername(), getHttpProxyPassword());
        }
        return null;
    }

    private InputStream loadConfigInputStream(String path, String name) throws WxPayException {
        if (StringUtils.isBlank(path)) {
            throw new WxPayException("请确认" + name + "值不为空");
        }
        try {
            if (path.startsWith("classpath:")) {
                String resource = path.substring("classpath:".length());
                if (!resource.startsWith("/")) {
                    resource = "/" + resource;
                }
                ClassPathResource classPathResource = new ClassPathResource(resource);
                return classPathResource.getInputStream();
            }
            return Files.newInputStream(Paths.get(path));
        } catch (Exception e) {
            throw new WxPayException("证书文件(" + name + ")读取失败，请检查路径: " + path, e);
        }
    }
}