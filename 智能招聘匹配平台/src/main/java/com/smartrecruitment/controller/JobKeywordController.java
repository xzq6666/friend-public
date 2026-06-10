package com.smartrecruitment.controller;

import com.smartrecruitment.entity.JobKeyword;
import com.smartrecruitment.service.JobKeywordService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 职位关键词管理控制器（仅管理员可用）
 */
@RestController
@RequestMapping("/admin/keywords")
@CrossOrigin
public class JobKeywordController {

    @Autowired
    private JobKeywordService jobKeywordService;

    /**
     * 获取所有关键词（按分类分组）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getKeywords() {
        Map<String, List<JobKeyword>> keywords = jobKeywordService.getKeywordsByCategory();
        return Map.of("success", true, "data", keywords);
    }

    /**
     * 获取热门关键词
     */
    @GetMapping("/top")
    public Map<String, Object> getTopKeywords(@RequestParam(defaultValue = "20") int limit) {
        List<JobKeyword> keywords = jobKeywordService.getTopKeywords(limit);
        return Map.of("success", true, "data", keywords);
    }

    /**
     * 手动添加关键词
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> addKeyword(@RequestBody Map<String, String> params) {
        String keyword = params.get("keyword");
        String category = params.get("category");
        String description = params.getOrDefault("description", "");

        if (keyword == null || keyword.isEmpty()) {
            return Map.of("success", false, "error", "关键词不能为空");
        }

        // 检查是否已存在
        JobKeyword existing = jobKeywordService.lambdaQuery()
            .eq(JobKeyword::getKeyword, keyword)
            .one();

        if (existing != null) {
            return Map.of("success", false, "error", "关键词已存在");
        }

        JobKeyword newKeyword = new JobKeyword();
        newKeyword.setKeyword(keyword);
        newKeyword.setCategory(category);
        newKeyword.setSource("MANUAL");
        newKeyword.setUsageCount(0);
        newKeyword.setIsActive(1);
        newKeyword.setDescription(description);
        jobKeywordService.save(newKeyword);

        return Map.of("success", true, "data", newKeyword);
    }

    /**
     * 更新关键词
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateKeyword(@PathVariable Long id, @RequestBody JobKeyword keyword) {
        keyword.setId(id);
        boolean success = jobKeywordService.updateById(keyword);
        if (success) {
            return Map.of("success", true);
        }
        return Map.of("success", false, "error", "更新失败");
    }

    /**
     * 删除关键词
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> deleteKeyword(@PathVariable Long id) {
        boolean success = jobKeywordService.removeById(id);
        if (success) {
            return Map.of("success", true);
        }
        return Map.of("success", false, "error", "删除失败");
    }

    /**
     * 批量导入关键词
     */
    @PostMapping("/batch-import")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> batchImport(@RequestBody List<Map<String, String>> keywords) {
        int successCount = 0;
        int failCount = 0;

        for (Map<String, String> params : keywords) {
            String keyword = params.get("keyword");
            String category = params.get("category");
            String description = params.getOrDefault("description", "");

            if (keyword == null || keyword.isEmpty()) {
                failCount++;
                continue;
            }

            JobKeyword existing = jobKeywordService.lambdaQuery()
                .eq(JobKeyword::getKeyword, keyword)
                .one();

            if (existing != null) {
                failCount++;
                continue;
            }

            JobKeyword newKeyword = new JobKeyword();
            newKeyword.setKeyword(keyword);
            newKeyword.setCategory(category);
            newKeyword.setSource("MANUAL");
            newKeyword.setUsageCount(0);
            newKeyword.setIsActive(1);
            newKeyword.setDescription(description);
            jobKeywordService.save(newKeyword);
            successCount++;
        }

        return Map.of(
            "success", true,
            "successCount", successCount,
            "failCount", failCount
        );
    }

    /**
     * 搜索关键词
     */
    @GetMapping("/search")
    public Map<String, Object> searchKeywords(@RequestParam String q) {
        List<JobKeyword> keywords = jobKeywordService.lambdaQuery()
            .like(JobKeyword::getKeyword, q)
            .eq(JobKeyword::getIsActive, 1)
            .orderByDesc(JobKeyword::getUsageCount)
            .last("LIMIT 50")
            .list();
        return Map.of("success", true, "data", keywords);
    }

    private static final String[] CATEGORY_OPTIONS = {
        "编程语言", "前端框架", "后端框架", "数据库", "开发工具", "概念", "行业", "软技能"
    };

    /**
     * 导出关键词列表为Excel
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportKeywords(HttpServletResponse response) throws IOException {
        List<JobKeyword> keywords = jobKeywordService.lambdaQuery()
            .orderByAsc(JobKeyword::getCategory)
            .orderByDesc(JobKeyword::getUsageCount)
            .list();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("关键词列表");

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "关键词", "分类", "来源", "使用次数", "状态", "描述", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (JobKeyword kw : keywords) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(kw.getId());
            row.createCell(1).setCellValue(kw.getKeyword() != null ? kw.getKeyword() : "");
            row.createCell(2).setCellValue(kw.getCategory() != null ? kw.getCategory() : "");
            row.createCell(3).setCellValue(kw.getSource() != null ? kw.getSource() : "");
            row.createCell(4).setCellValue(kw.getUsageCount() != null ? kw.getUsageCount() : 0);
            row.createCell(5).setCellValue(kw.getIsActive() != null && kw.getIsActive() == 1 ? "启用" : "禁用");
            row.createCell(6).setCellValue(kw.getDescription() != null ? kw.getDescription() : "");
            row.createCell(7).setCellValue(kw.getCreateTime() != null ? kw.getCreateTime().toString() : "");
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode("关键词列表.xlsx", StandardCharsets.UTF_8));
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 下载关键词导入模板
     */
    @GetMapping("/export-template")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportTemplate(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("关键词导入模板");

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"关键词 *", "分类 *", "描述"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 分类下拉校验
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(CATEGORY_OPTIONS);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, 1000, 1, 1));
        validation.createErrorBox("输入错误", "请从下拉列表中选择分类");
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);

        // 示例数据行
        String[][] examples = {
            {"Java", "编程语言", "Java编程语言"},
            {"React", "前端框架", "React前端框架"},
            {"MySQL", "数据库", "MySQL关系型数据库"},
        };
        for (int i = 0; i < examples.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < examples[i].length; j++) {
                row.createCell(j).setCellValue(examples[i][j]);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode("关键词导入模板.xlsx", StandardCharsets.UTF_8));
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 从Excel文件导入关键词
     */
    @PostMapping("/import-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Map.of("success", false, "error", "请上传文件");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.endsWith(".xlsx") && !originalName.endsWith(".xls"))) {
            return Map.of("success", false, "error", "仅支持 .xlsx 或 .xls 格式的文件");
        }

        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String keyword = getCellStringValue(row.getCell(0)).trim();
                String category = getCellStringValue(row.getCell(1)).trim();
                String description = getCellStringValue(row.getCell(2)).trim();

                if (keyword.isEmpty() || category.isEmpty()) {
                    failCount++;
                    continue;
                }

                JobKeyword existing = jobKeywordService.lambdaQuery()
                    .eq(JobKeyword::getKeyword, keyword)
                    .one();

                if (existing != null) {
                    duplicateCount++;
                    continue;
                }

                JobKeyword newKeyword = new JobKeyword();
                newKeyword.setKeyword(keyword);
                newKeyword.setCategory(category);
                newKeyword.setSource("MANUAL");
                newKeyword.setUsageCount(0);
                newKeyword.setIsActive(1);
                newKeyword.setDescription(description);
                jobKeywordService.save(newKeyword);
                successCount++;
            }
        } catch (IOException e) {
            return Map.of("success", false, "error", "文件读取失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("successCount", successCount);
        result.put("duplicateCount", duplicateCount);
        result.put("failCount", failCount);
        return result;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return "";
    }
}
