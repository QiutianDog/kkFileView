package cn.keking.config;

import cn.keking.utils.AESUtil;
import freemarker.template.TemplateMethodModelEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * 为 Freemarker 注入 aesEncode() 共享函数。
 *
 * 目的：预览模板调用 getCorsFile 时，源文件地址必须由服务端加密后下发。
 * 浏览器侧不允许再自行 base64 拼接，否则等于保留了一条绕过 AES 的通道，
 * 并且会把 AES 密钥暴露到前端。模板用法：
 *
 *   urlPath = ${aesEncode(finalUrl)}
 *
 * @author kkfileview
 */
@Component
public class FreemarkerAesConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreemarkerAesConfig.class);

    public FreemarkerAesConfig(FreeMarkerConfigurer freeMarkerConfigurer) {
        freeMarkerConfigurer.getConfiguration().setSharedVariable("aesEncode",
                (TemplateMethodModelEx) arguments -> {
                    if (arguments == null || arguments.isEmpty() || arguments.get(0) == null) {
                        return "";
                    }
                    String encrypted = AESUtil.aesEncrypt(String.valueOf(arguments.get(0)));
                    return encrypted == null ? "" : encrypted;
                });
        LOGGER.info("Freemarker aesEncode() 共享函数已注册");
    }
}
