package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.AESUtil;
import cn.keking.utils.DownloadUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.service.CompressFileReader;
import cn.keking.service.ZtreeNodeVo;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.RarUtils;
import cn.keking.web.filter.BaseUrlFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.EncryptedDocumentException;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :处理压缩包文件
 */
@Service
public class CompressFilePreviewImpl implements FilePreview {

    private final FileHandlerService fileHandlerService;
    private final CompressFileReader compressFileReader;
    private final OtherFilePreviewImpl otherFilePreview;
    private static final String Rar_PASSWORD_MSG = "password";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CompressFileReader.class);


    public CompressFilePreviewImpl(FileHandlerService fileHandlerService, CompressFileReader compressFileReader, OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.compressFileReader = compressFileReader;
        this.otherFilePreview = otherFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        String fileName = fileAttribute.getName();
        String filePassword = fileAttribute.getFilePassword();
        boolean forceUpdatedCache = fileAttribute.forceUpdatedCache();
        String fileTree = null;
        // 判断文件名是否存在(redis缓存读取)
        if (forceUpdatedCache || !StringUtils.hasText(fileHandlerService.getConvertedFile(fileName)) || !ConfigConstants.isCacheEnabled()) {
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
            if (response.isFailure()) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
            }
            String filePath = response.getContent();
            try {
                fileTree = compressFileReader.unRar(filePath, filePassword, fileName, fileAttribute);
            } catch (Exception e) {
                if (e.getMessage().toLowerCase().contains(Rar_PASSWORD_MSG)) {
                    model.addAttribute("needFilePassword", true);
                    return EXEL_FILE_PREVIEW_PAGE;
                }else {
                    logger.error("Error processing RAR file: " + e.getMessage(), e);
                }
            }
            if (!ObjectUtils.isEmpty(fileTree)) {
                //是否保留压缩包源文件
                if (!fileAttribute.isCompressFile() && ConfigConstants.getDeleteSourceFile()) {
                    KkFileUtils.deleteFileByPath(filePath);
                }
                if (ConfigConstants.isCacheEnabled()) {
                    // 加入缓存
                    fileHandlerService.addConvertedFile(fileName, fileTree);
                }
            } else {
                return otherFilePreview.notSupportedFile(model, fileAttribute, "该压缩包文件无法处理!");
            }
        } else {
            fileTree = fileHandlerService.getConvertedFile(fileName);
        }
        model.addAttribute("fileName", fileName);
        model.addAttribute("fileTree", fileTree);
        try {
            List<ZtreeNodeVo> archiveTree = RarUtils.getTree(fileTree);
            fillAesPath(archiveTree, fileTree);
            model.addAttribute("archiveTreeJson", new ObjectMapper().writeValueAsString(archiveTree));
        } catch (IOException | SecurityException e) {
            logger.warn("Unable to read the previewed archive directory", e);
            return otherFilePreview.notSupportedFile(model, fileAttribute, "该压缩包目录无法读取!");
        }
        return COMPRESS_FILE_PREVIEW_PAGE;
    }

    /**
     * 为压缩包内每个节点预先计算 AES 加密后的嵌套预览地址。
     * 页面侧不允许再自行 base64 拼接，否则会保留一条绕过 AES 的通道。
     */
    private void fillAesPath(List<ZtreeNodeVo> nodes, String fileTree) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        String baseUrl = BaseUrlFilter.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "/";
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        for (ZtreeNodeVo node : nodes) {
            if (node == null) {
                continue;
            }
            String path = baseUrl + node.id
                    + "?kkCompressfileKey=" + fileTree
                    + "&kkCompressfilepath=" + encode(node.id)
                    + "&fullfilename=" + encode(node.name);
            node.aesPath = AESUtil.aesEncrypt(path);
            fillAesPath(node.children, fileTree);
        }
    }

    private String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
