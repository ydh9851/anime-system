package com.alvis.media.controller.admin;


import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.configuration.property.SystemConfig;
import com.alvis.media.service.FileUpload;
import com.alvis.media.service.UserService;
import com.alvis.media.viewmodel.admin.file.UeditorConfigVM;
import com.alvis.media.viewmodel.admin.file.UploadResultVM;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
@AllArgsConstructor
@RequestMapping("/api/admin/upload")
@RestController("AdminUploadController")
public class UploadController extends BaseApiController {

    private final FileUpload fileUpload;
    private final SystemConfig systemConfig;
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private static final String IMAGE_UPLOAD = "imgUpload";
    private static final String IMAGE_UPLOAD_FILE = "upFile";
    private final UserService userService;

    /**
     * UEditor 富编辑器「配置 + 上传」聚合接口。
     *
     * 注意：返回体是 UEditor 前端约定的扁平结构（state / url 等），
     * 不套RestResponse —— 这是第三方组件协议，改成统一包装会导致编辑器取不到 url。
     * 上传失败时返回 state=FAIL 而不是 null，避免前端拿到 undefined 报未知错误。
     */
    @ResponseBody
    @RequestMapping("/configAndUpload")
    public Object upload(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getParameter("action");
        if (!IMAGE_UPLOAD.equals(action)) {
            UeditorConfigVM ueditorConfigVM = new UeditorConfigVM();
            ueditorConfigVM.setImageActionName(IMAGE_UPLOAD);
            ueditorConfigVM.setImageFieldName(IMAGE_UPLOAD_FILE);
            ueditorConfigVM.setImageMaxSize(2048000L);
            ueditorConfigVM.setImageAllowFiles(Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp"));
            ueditorConfigVM.setImageCompressEnable(true);
            ueditorConfigVM.setImageCompressBorder(1600);
            ueditorConfigVM.setImageInsertAlign("none");
            ueditorConfigVM.setImageUrlPrefix("");
            ueditorConfigVM.setImagePathFormat("");
            return ueditorConfigVM;
        }

        UploadResultVM result = new UploadResultVM();
        try {
            MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
            MultipartFile multipartFile = multipartHttpServletRequest.getFile(IMAGE_UPLOAD_FILE);
            if (multipartFile == null || multipartFile.isEmpty()) {
                result.setState("FAIL");
                logger.warn("UEditor 上传未收到文件");
                return result;
            }
            String imgName = multipartFile.getOriginalFilename();
            String filePath;
            try (InputStream inputStream = multipartFile.getInputStream()) {
                filePath = fileUpload.uploadFile(inputStream, multipartFile.getSize(), imgName);
            }
            result.setOriginal(imgName);
            result.setName(imgName);
            result.setUrl(filePath);
            result.setSize(multipartFile.getSize());
            result.setType(imgName.substring(imgName.lastIndexOf(".")));
            result.setState("SUCCESS");
        } catch (IOException | IllegalArgumentException e) {
            // 详细信息只进日志，响应体不暴露服务器路径
            logger.error("UEditor 图片上传失败", e);
            result.setState("FAIL");
        }
        return result;
    }


    @PostMapping("/image")
    @ResponseBody
    public RestResponse<String> questionUploadAndReadExcel(HttpServletRequest request) {
        MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartHttpServletRequest.getFile("file");
        if (multipartFile == null || multipartFile.isEmpty()) {
            return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "请选择要上传的图片");
        }
        try (InputStream inputStream = multipartFile.getInputStream()) {
            String filePath = fileUpload.uploadFile(inputStream, multipartFile.getSize(), multipartFile.getOriginalFilename());
            if (filePath == null) {
                return RestResponse.fail(SystemCode.InnerError.getCode(), "图片上传失败");
            }
            userService.changePicture(getCurrentUser(), filePath);
            return RestResponse.ok(filePath);
        } catch (IOException | IllegalArgumentException e) {
            // 异常信息可能含服务器绝对路径，只进日志不对外返回
            logger.error("头像上传失败", e);
            return RestResponse.fail(SystemCode.InnerError.getCode(), "图片上传失败");
        }
    }

    @GetMapping("/auth")
    @ResponseBody
    public RestResponse<Void> auth() {
        return RestResponse.ok();
    }

    /**
     * 视频文件上传：按 SavePath 落盘，返回重构后的文件名与可访问路径。
     */
    @PostMapping(value = "/uploadVidoe")
    @ResponseBody
    public RestResponse<Map<String, String>> savaVideo(@RequestParam("file") MultipartFile file,
                                                     @RequestParam String SavePath) {
        Map<String, String> resultMap = new HashMap<>();
        if (file == null || file.isEmpty()) {
            return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "请选择要上传的视频");
        }
        String originalName = file.getOriginalFilename();
        try {
            // 取扩展名，重构文件名，避免同名覆盖
            String fileExt = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            String newVidoeName = UUID.randomUUID().toString().replace("-", "") + "." + fileExt;
            File fileSave = new File(SavePath, newVidoeName);
            // 目录不存在时自动创建，否则 transferTo 会报“系统找不到指定的路径”导致上传失败
            File parentDir = fileSave.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("创建目录失败");
            }
            // 用流拷贝而非 transferTo：Undertow 的 transferTo 在临时文件清理时序下会抛
            // NoSuchFileException，这里保证两种容器行为一致
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, fileSave.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            resultMap.put("newVidoeName", newVidoeName);
            resultMap.put("VideoUrl", SavePath + "/" + newVidoeName);
            return RestResponse.ok(resultMap);
        } catch (Exception e) {
            logger.error("视频上传失败, SavePath={}, 文件名={}", SavePath, originalName, e);
            return RestResponse.fail(SystemCode.InnerError.getCode(), "视频上传失败");
        }
    }
}
