package com.qmz.canteen.controller;

import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传：管理员/商家上传菜品图、广告图，存本地 uploads 目录，返回 /uploads/xxx 访问地址。
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Value("${canteen.upload-dir:uploads}")
    private String uploadDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (!UserContext.isAdmin() && !UserContext.isMerchant()) {
            throw new BusinessException(403, "只有管理员或商家可以上传图片");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(400, "仅支持 jpg/png/webp/gif 图片");
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            return Result.success(contextPath + "/uploads/" + filename);
        } catch (Exception e) {
            throw new BusinessException(500, "图片保存失败，请重试");
        }
    }
}
