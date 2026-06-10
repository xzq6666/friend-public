package com.smartrecruitment.service;

import com.aliyun.oss.OSS;
import com.smartrecruitment.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * ================================================
 * 文件上传服务
 * ================================================
 *
 * 功能说明：
 *   - 支持本地存储（默认）和阿里云 OSS 存储两种模式
 *   - 本地存储路径：./uploads/resumes/{yyyy/MM/dd}/{uuid}.{ext}
 *   - OSS 存储路径：resumes/{yyyy/MM/dd}/{uuid}.{ext}
 *   - 通过 application.yml 中的 oss.enabled 切换
 *
 * 如需扩展：
 *   - 添加文件类型白名单校验
 *   - 添加文件大小限制（已在 yml 中配置）
 *   - 添加图片缩略图生成
 *   - 添加文件访问鉴权 URL 生成
 *   - 添加定期清理临时文件
 */
@Service
public class FileUploadService {

    @Value("${upload.local-path:./uploads}")
    private String localUploadPath;

    @Autowired(required = false)
    private OSS ossClient;

    @Autowired(required = false)
    private OssConfig ossConfig;

    @Value("${oss.enabled:false}")
    private boolean ossEnabled;

    @PostConstruct
    public void init() {
        // 确保本地存储目录存在
        if (!ossEnabled) {
            File dir = new File(localUploadPath + "/resumes");
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    /**
     * 上传文件，返回可访问的 URL
     */
    public String upload(MultipartFile file) {
        if (ossEnabled && ossClient != null && ossConfig != null) {
            return uploadToOss(file);
        }
        return uploadToLocal(file);
    }

    /**
     * 本地存储
     */
    private String uploadToLocal(MultipartFile file) {
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String originalName = file.getOriginalFilename();
            String ext = getExtension(originalName);
            // 保留原始文件名前缀，增加日期和UUID防止重名
            String prefix = originalName != null && originalName.contains(".") 
                ? originalName.substring(0, originalName.lastIndexOf(".")) 
                : "resume";
            // 限制前缀长度
            if (prefix.length() > 20) prefix = prefix.substring(0, 20);
            String filename = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            String relativePath = "resumes/" + dateDir + "/" + filename;

            Path fullPath = Paths.get(localUploadPath, relativePath);
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, file.getBytes());

            return "/uploads/" + relativePath;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 阿里云 OSS 存储
     */
    private String uploadToOss(MultipartFile file) {
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String objectName = "resumes/" + dateDir + "/" + filename;

            ossClient.putObject(ossConfig.getBucketName(), objectName, file.getInputStream());

            return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectName;
        } catch (IOException e) {
            throw new RuntimeException("OSS上传失败: " + e.getMessage(), e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
