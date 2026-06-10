package com.smartrecruitment.service;

import com.smartrecruitment.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文档解析服务接口
 * 支持解析 PDF、DOC、DOCX 格式的简历文档
 */
public interface DocumentParseService {

    /**
     * 解析简历文档，提取结构化信息
     * @param file 上传的文件
     * @return 解析后的结构化数据
     */
    Map<String, Object> parseResumeDocument(MultipartFile file);

    /**
     * 解析简历文档并填充到 Resume 实体
     * @param file 上传的文件
     * @return 填充后的 Resume 实体
     */
    Resume parseResumeToEntity(MultipartFile file);

    /**
     * 从文件路径解析文档内容
     * @param filePath 文件路径
     * @param fileName 文件名
     * @return 文档文本内容
     */
    String extractTextFromFile(String filePath, String fileName);

    /**
     * 直接从字节数组提取文本（避免磁盘IO，更快）
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return 文档文本内容
     */
    String extractTextFromMultipartFileBytes(byte[] fileBytes, String fileName);
}
