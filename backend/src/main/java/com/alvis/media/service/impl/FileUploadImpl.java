package com.alvis.media.service.impl;

import com.alvis.media.configuration.property.QnConfig;
import com.alvis.media.configuration.property.SystemConfig;
import com.alvis.media.service.FileUpload;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传实现。
 *
 * 优先走七牛云对象存储；未配置 system.qn.access-key/secret-key/bucket 时
 * 自动回退到本地磁盘存储（data/upload），避免未配置 AK 时直接抛 NPE 导致接口不可用。
 */
@Service
public class FileUploadImpl implements FileUpload {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadImpl.class);

    /** 未配置七牛时本地落盘的访问前缀，需与 WebMvcConfiguration 的资源映射一致 */
    public static final String LOCAL_URL_PREFIX = "/api/admin/upload/local/";

    /** 本地存储白名单扩展名（图片与视频） */
    private static final Set<String> ALLOWED_EXT = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "webp",
            "mp4", "webm", "ogg", "avi", "mkv", "mov");

    private final SystemConfig systemConfig;

    @Value("${system.upload.local-dir:../data/upload}")
    private String localDir;

    public FileUploadImpl(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) {
        String safeName = sanitize(extName);
        QnConfig qnConfig = systemConfig.getQn();
        if (qnConfig != null && StringUtils.hasText(qnConfig.getAccessKey())
                && StringUtils.hasText(qnConfig.getSecretKey())
                && StringUtils.hasText(qnConfig.getBucket())) {
            String url = uploadToQiniu(inputStream, qnConfig);
            if (url != null) {
                return url;
            }
            logger.warn("七牛上传失败，回退到本地存储");
        }
        return saveLocal(inputStream, safeName);
    }

    private String uploadToQiniu(InputStream inputStream, QnConfig qnConfig) {
        try {
            Configuration cfg = new Configuration(Region.region2());
            UploadManager uploadManager = new UploadManager(cfg);
            Auth auth = Auth.create(qnConfig.getAccessKey(), qnConfig.getSecretKey());
            String upToken = auth.uploadToken(qnConfig.getBucket());
            Response response = uploadManager.put(inputStream, null, upToken, null, null);
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            return qnConfig.getUrl() + "/" + putRet.key;
        } catch (QiniuException e) {
            logger.error("七牛上传异常", e);
            return null;
        }
    }

    /**
     * 本地落盘：data/upload/yyyyMMdd/<uuid>.<ext>
     */
    private String saveLocal(InputStream inputStream, String fileName) {
        try {
            Path dir = Paths.get(localDir, LocalDate.now().toString().replace("-", ""));
            Files.createDirectories(dir);
            String newName = UUID.randomUUID().toString().replace("-", "")
                    + fileName.substring(fileName.lastIndexOf("."));
            Path target = dir.resolve(newName).normalize();
            if (!target.startsWith(dir.normalize())) {
                throw new IllegalArgumentException("非法文件名");
            }
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            String relative = dir.getFileName().toString() + "/" + newName;
            logger.info("文件已保存到本地: {}", target.toAbsolutePath());
            return LOCAL_URL_PREFIX + relative;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("本地文件保存失败", e);
            return null;
        }
    }

    /**
     * 文件名安全化：只取 base 名、校验扩展名白名单，杜绝 ../../ 路径穿越与可执行文件上传
     */
    private String sanitize(String originalName) {
        // Spring 6 起 StringUtils.clean 已移除，用 getFilename 直接取 basename，
        // 天然阻断 ../../ 之类的路径穿越
        String name = StringUtils.getFilename(originalName);
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int dot = name.lastIndexOf(".");
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (dot < 0 || !ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }
        return name;
    }
}
