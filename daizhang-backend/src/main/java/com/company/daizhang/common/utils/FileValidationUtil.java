package com.company.daizhang.common.utils;

import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 文件安全校验工具:大小、扩展名白名单、防路径穿越、Magic Number 校验。
 * 抽取自 VoucherAttachmentServiceImpl,供所有需要文件上传的模块复用(DRY)。
 */
public final class FileValidationUtil {

    /** 默认最大文件大小:10MB */
    public static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** 默认允许的扩展名白名单 */
    public static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "xls", "xlsx", "doc", "docx", "txt", "zip"
    );

    private FileValidationUtil() {}

    /**
     * 使用默认参数校验文件
     */
    public static void validate(MultipartFile file) {
        validate(file, DEFAULT_MAX_FILE_SIZE, DEFAULT_ALLOWED_EXTENSIONS);
    }

    /**
     * 校验文件:大小、扩展名白名单、防路径穿越、Magic Number
     *
     * @param file           上传的文件
     * @param maxSize        最大字节数
     * @param allowedExts    允许的扩展名集合(小写)
     */
    public static void validate(MultipartFile file, long maxSize, Set<String> allowedExts) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小不能超过" + (maxSize / 1024 / 1024) + "MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        // 防路径穿越:仅取文件名部分
        String filename = originalFilename;
        if (filename.contains("/") || filename.contains("\\")) {
            filename = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        }
        if (filename.contains("..")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名非法");
        }
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!allowedExts.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的文件类型,允许: " + String.join(",", allowedExts));
        }

        // Magic Number 校验(txt 无固定 magic number,跳过)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read > 0) {
                validateMagicNumber(header, read, extension);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件读取失败");
        }
    }

    /**
     * 校验文件头 Magic Number 与扩展名是否匹配
     */
    private static void validateMagicNumber(byte[] header, int length, String extension) {
        // PDF: %PDF-
        boolean pdfHeader = length >= 5
                && header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44
                && header[3] == 0x46 && header[4] == 0x2D;
        // PNG: 89 50 4E 47
        boolean pngHeader = length >= 8
                && header[0] == (byte) 0x89 && header[1] == 0x50
                && header[2] == 0x4E && header[3] == 0x47;
        // JPG/JPEG: FF D8 FF
        boolean jpgHeader = length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
        // GIF: 47 49 46 38
        boolean gifHeader = length >= 4
                && header[0] == 0x47 && header[1] == 0x49
                && header[2] == 0x46 && header[3] == 0x38;
        // ZIP-based (xls/xlsx/doc/docx/zip): 50 4B 03 04
        boolean zipHeader = length >= 4
                && header[0] == 0x50 && header[1] == 0x4B
                && header[2] == 0x03 && header[3] == 0x04;

        switch (extension) {
            case "pdf":
                if (!pdfHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(pdf)");
                }
                break;
            case "png":
                if (!pngHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(png)");
                }
                break;
            case "jpg":
            case "jpeg":
                if (!jpgHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(jpg)");
                }
                break;
            case "gif":
                if (!gifHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(gif)");
                }
                break;
            case "xls":
            case "xlsx":
            case "doc":
            case "docx":
            case "zip":
                if (!zipHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(office/zip)");
                }
                break;
            case "txt":
                // 无固定 magic number,跳过
                break;
            default:
                break;
        }
    }
}
