package com.bisai.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 上传文件安全校验工具。
 * 规则：
 * 1. 文件名只允许单个扩展名（shell.php.jpg / xss.html.txt 这类多段扩展名直接拒绝）；
 * 2. 文件名不允许路径分隔符、控制字符、首尾点/空格；
 * 3. 扩展名必须在白名单内；
 * 4. 可识别类型的文件做文件头（magic number）校验，防止改扩展名伪装。
 * 存储侧配合：落盘一律 UUID 重命名 + 白名单扩展名（见各 Service）。
 */
public final class FileValidationUtil {

    private FileValidationUtil() {
    }

    /**
     * 校验原始文件名与扩展名白名单。
     *
     * @return null 表示通过，否则为错误信息
     */
    public static String validateFileName(String originalName, Set<String> allowedExtensions) {
        if (originalName == null || originalName.isBlank()) {
            return "文件名不能为空";
        }
        // 长度上限：对齐文件系统 255 字节上限，防止超长文件名穿透到落盘/DB
        if (originalName.length() > 255) {
            return "文件名过长（上限 255 字符）";
        }
        // 去掉浏览器可能带的路径部分后校验原始串，任何路径分隔符都拒绝
        if (originalName.contains("/") || originalName.contains("\\")) {
            return "文件名不允许包含路径分隔符";
        }
        if (originalName.chars().anyMatch(c -> c < 32 || c == 127)) {
            return "文件名包含非法字符";
        }
        if (originalName.startsWith(".") || originalName.endsWith(".") || originalName.endsWith(" ")) {
            return "文件名不能以点开头或以点/空格结尾";
        }
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot <= 0) {
            return "文件必须带有扩展名";
        }
        String base = originalName.substring(0, lastDot);
        // 双扩展名防御：主名（去掉最后一个扩展名后）不允许再含点，如 shell.php.jpg
        if (base.contains(".")) {
            return "文件名仅允许单个扩展名";
        }
        String ext = originalName.substring(lastDot + 1).toUpperCase(Locale.ROOT);
        if (ext.isBlank() || !allowedExtensions.contains(ext)) {
            return "不支持的文件类型: " + originalName.substring(lastDot);
        }
        return null;
    }

    /**
     * 文件头（magic number）校验，防止伪造扩展名。无法识别内容的类型（TXT/MD）跳过。
     *
     * @return null 表示通过，否则为错误信息
     */
    public static String verifyMagicNumber(MultipartFile file, String extUpper) {
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, 8);
        } catch (IOException e) {
            return "文件读取失败";
        }
        if (read <= 0) {
            return "文件内容为空";
        }
        switch (extUpper) {
            case "JPG", "JPEG" -> {
                if (!startsWith(head, read, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) return "文件内容与扩展名不符（非 JPEG 图片）";
            }
            case "PNG" -> {
                if (!startsWith(head, read, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A))
                    return "文件内容与扩展名不符（非 PNG 图片）";
            }
            case "PDF" -> {
                if (!startsWith(head, read, (byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46)) return "文件内容与扩展名不符（非 PDF 文档）";
            }
            case "DOCX", "XLSX", "PPTX", "ZIP" -> {
                if (!startsWith(head, read, (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04)) return "文件内容与扩展名不符（非合法的 " + extUpper + " 文件）";
            }
            case "DOC", "XLS", "PPT" -> {
                // OLE2 复合文档头；docx 改名的 doc 也按内容放行（均为 Office 系）
                if (!startsWith(head, read, (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0)
                        && !startsWith(head, read, (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04))
                    return "文件内容与扩展名不符（非合法的 " + extUpper + " 文件）";
            }
            default -> {
                // TXT/MD 等纯文本类型无稳定文件头，跳过
            }
        }
        return null;
    }

    /**
     * zip 类容器安全校验（DOCX/XLSX/PPTX/ZIP）：只读中央目录元数据、不解压，
     * 拦截 zip 炸弹（如 634KB 压缩 → 161MB 解压的 DOCX）。规则：
     * 单条目解压后 >100MB、总解压量 >200MB、或压缩比 >100:1（压缩侧≥64KB 时）即拒绝。
     *
     * @return null 表示通过，否则为错误信息
     */
    public static String verifyZipSafety(MultipartFile file, String extUpper) {
        switch (extUpper) {
            case "DOCX", "XLSX", "PPTX", "ZIP" -> { /* 需要检查 */ }
            default -> { return null; }
        }
        java.io.File temp = null;
        try {
            temp = java.io.File.createTempFile("zipguard-", ".tmp");
            // 用流拷贝而非 transferTo：后者会移动 servlet 临时文件，导致后续落盘/MD5 读取失败
            java.nio.file.Files.copy(file.getInputStream(), temp.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            long total = 0;
            try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(temp)) {
                var entries = zf.entries();
                while (entries.hasMoreElements()) {
                    var e = entries.nextElement();
                    long size = e.getSize();
                    long csize = e.getCompressedSize();
                    if (size > 100L * 1024 * 1024) {
                        return "压缩包内文件解压后过大（>100MB）: " + e.getName();
                    }
                    total += Math.max(0, size);
                    if (total > 200L * 1024 * 1024) {
                        return "压缩包总解压量过大（>200MB）";
                    }
                    if (csize > 64L * 1024 && size > csize * 100) {
                        return "压缩比异常（>100:1），疑似压缩炸弹: " + e.getName();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return "压缩包读取失败: " + e.getMessage();
        } finally {
            if (temp != null && temp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
            }
        }
    }

    private static boolean startsWith(byte[] data, int len, byte... prefix) {
        if (len < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
