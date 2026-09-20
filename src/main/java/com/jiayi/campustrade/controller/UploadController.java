package com.jiayi.campustrade.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传接口
 *
 * 设计说明：
 * 1. 只负责"文件落盘 + 返回可访问 URL"，图片与商品的关联由
 *    POST /goods/{id}/images 在商品保存时写入 goods_image 表（职责分离）
 * 2. 文件名使用 UUID 重新生成，不使用用户原始文件名：
 *    - 避免重名覆盖、避免中文/特殊字符路径问题
 *    - 避免通过文件名注入路径（安全）
 * 3. 数据库只存 URL，不存图片二进制；本地上传目录通过静态资源映射对外访问。
 *    真实生产环境通常会替换为 OSS/COS/S3/MinIO 对象存储，接口形态不变
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    // 上传根目录（application-local.yml 中配置）
    @Value("${app.upload.dir:D:/campus-trade-uploads}")
    private String uploadDir;

    // 允许的图片扩展名白名单
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    // 单文件大小上限：5MB（与 spring.servlet.multipart.max-file-size 保持一致）
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    /**
     * 上传商品图片：POST /api/upload/image（multipart/form-data，字段名 file）
     * 成功返回 { url: "/api/files/goods/xxx.jpg" }，失败返回 { error: 原因 }
     * 项目既有风格：HTTP 200 + 文本字段表达业务结果，前端按字段判断
     */
    @PostMapping("/image")
    public Map<String, String> uploadImage(
            @RequestParam("file") MultipartFile file) {

        Map<String, String> result = new HashMap<>();

        // 1. 基础校验：文件存在
        if (file == null || file.isEmpty()) {
            result.put("error", "请选择要上传的图片");
            return result;
        }

        // 2. 大小校验
        if (file.getSize() > MAX_SIZE) {
            result.put("error", "图片大小不能超过 5MB");
            return result;
        }

        // 3. 扩展名白名单校验（小写比较，防止 .JPG 绕过）
        String originalName = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename();
        int dotIndex = originalName.lastIndexOf('.');
        String extension = dotIndex < 0
                ? "" : originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            result.put("error", "仅支持 jpg/jpeg/png/gif/webp 格式图片");
            return result;
        }

        // 4. 文件头魔数校验：防止把可执行文件/脚本改后缀伪装成图片
        if (!isRealImage(file)) {
            result.put("error", "文件内容不是有效图片");
            return result;
        }

        // 5. 生成新文件名：UUID + 原扩展名，落到 goods 子目录
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Path dir = Paths.get(uploadDir, "goods");
            Files.createDirectories(dir);

            File target = dir.resolve(newFileName).toFile();
            file.transferTo(target);

        } catch (IOException e) {
            result.put("error", "图片保存失败，请稍后再试");
            return result;
        }

        // 6. 返回相对 URL：不带域名端口，前端 <img src> 与浏览器同源，
        //    dev 环境走 Vite 代理，生产环境走 Nginx，换部署地址无需迁移数据库数据
        result.put("url", "/api/files/goods/" + newFileName);

        return result;

    }

    /**
     * 读取文件头魔数判断真实图片类型
     * JPEG: FF D8 FF / PNG: 89 50 4E 47 / GIF: GIF8 / WEBP: RIFF....WEBP
     */
    private boolean isRealImage(MultipartFile file) {

        byte[] header = new byte[12];

        try (InputStream in = file.getInputStream()) {
            int read = in.read(header);
            if (read < 4) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        boolean isJpeg = (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;

        boolean isPng = (header[0] & 0xFF) == 0x89
                && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;

        boolean isGif = header[0] == 'G' && header[1] == 'I'
                && header[2] == 'F' && header[3] == '8';

        boolean isWebP = header[0] == 'R' && header[1] == 'I'
                && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E'
                && header[10] == 'B' && header[11] == 'P';

        return isJpeg || isPng || isGif || isWebP;

    }
}
