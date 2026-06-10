package com.smartrecruitment.service;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.MatchRecord;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * 数据导出服务
 */
@Service
public class DataExportService {

    @Autowired
    private UserService userService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private MatchRecordService matchRecordService;

    /**
     * 获取预计导出记录数
     */
    public long getExportCount(String type, String startDate, String endDate) {
        return switch (type) {
            case "users" -> {
                QueryWrapper<User> wrapper = new QueryWrapper<>();
                if (startDate != null && endDate != null) {
                    wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
                }
                yield userService.count(wrapper);
            }
            case "resumes" -> {
                QueryWrapper<Resume> wrapper = new QueryWrapper<>();
                if (startDate != null && endDate != null) {
                    wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
                }
                yield resumeService.count(wrapper);
            }
            case "jobs" -> {
                QueryWrapper<Job> wrapper = new QueryWrapper<>();
                if (startDate != null && endDate != null) {
                    wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
                }
                yield jobService.count(wrapper);
            }
            case "match-records" -> {
                QueryWrapper<MatchRecord> wrapper = new QueryWrapper<>();
                if (startDate != null && endDate != null) {
                    wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
                }
                yield matchRecordService.count(wrapper);
            }
            default -> throw new IllegalArgumentException("不支持的导出类型: " + type);
        };
    }

    /**
     * 导出用户数据
     */
    public void exportUsers(HttpServletResponse response, String format, String fields, String startDate, String endDate) throws IOException {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (startDate != null && endDate != null) {
            wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
        }
        List<User> users = userService.list(wrapper);
        
        String[] selectedFields = (fields != null && !fields.isEmpty()) ? fields.split(",") : 
            new String[]{"id", "username", "email", "phone", "userType", "createTime", "status"};
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户列表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < selectedFields.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(getFieldLabel("user", selectedFields[i]));
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < selectedFields.length; i++) {
                row.createCell(i).setCellValue(getUserFieldValue(user, selectedFields[i]));
            }
        }

        // 自动调整列宽
        for (int i = 0; i < selectedFields.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "用户数据", format);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导出简历数据
     */
    public void exportResumes(HttpServletResponse response, String format, String fields, String startDate, String endDate) throws IOException {
        QueryWrapper<Resume> wrapper = new QueryWrapper<>();
        if (startDate != null && endDate != null) {
            wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
        }
        List<Resume> resumes = resumeService.list(wrapper);
        
        String[] selectedFields = (fields != null && !fields.isEmpty()) ? fields.split(",") : 
            new String[]{"id", "name", "age", "education", "skills", "workExperience", "expectedSalary", "createTime"};
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("简历列表");

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < selectedFields.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(getFieldLabel("resume", selectedFields[i]));
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Resume resume : resumes) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < selectedFields.length; i++) {
                row.createCell(i).setCellValue(getResumeFieldValue(resume, selectedFields[i]));
            }
        }

        for (int i = 0; i < selectedFields.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "简历数据", format);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导出职位数据
     */
    public void exportJobs(HttpServletResponse response, String format, String fields, String startDate, String endDate) throws IOException {
        QueryWrapper<Job> wrapper = new QueryWrapper<>();
        if (startDate != null && endDate != null) {
            wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
        }
        List<Job> jobs = jobService.list(wrapper);
        
        String[] selectedFields = (fields != null && !fields.isEmpty()) ? fields.split(",") : 
            new String[]{"id", "title", "company", "location", "salaryMin", "salaryMax", "experienceRequired", "educationRequired", "status", "createTime"};
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("职位列表");

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < selectedFields.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(getFieldLabel("job", selectedFields[i]));
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Job job : jobs) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < selectedFields.length; i++) {
                row.createCell(i).setCellValue(getJobFieldValue(job, selectedFields[i]));
            }
        }

        for (int i = 0; i < selectedFields.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "职位数据", format);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导出匹配记录数据
     */
    public void exportMatchRecords(HttpServletResponse response, String format, String fields, String startDate, String endDate) throws IOException {
        QueryWrapper<MatchRecord> wrapper = new QueryWrapper<>();
        if (startDate != null && endDate != null) {
            wrapper.between("create_time", LocalDate.parse(startDate).atStartOfDay(), LocalDate.parse(endDate).atTime(LocalTime.MAX));
        }
        List<MatchRecord> records = matchRecordService.list(wrapper);
        
        String[] selectedFields = (fields != null && !fields.isEmpty()) ? fields.split(",") : 
            new String[]{"id", "jobTitle", "candidateName", "matchScore", "skillsScore", "experienceScore", "educationScore", "createTime"};
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("匹配记录");

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < selectedFields.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(getFieldLabel("match", selectedFields[i]));
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (MatchRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < selectedFields.length; i++) {
                row.createCell(i).setCellValue(getMatchRecordFieldValue(record, selectedFields[i]));
            }
        }

        for (int i = 0; i < selectedFields.length; i++) {
            sheet.autoSizeColumn(i);
        }

        setResponseHeader(response, "匹配记录", format);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private void setResponseHeader(HttpServletResponse response, String fileName, String format) throws IOException {
        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName + ".csv", StandardCharsets.UTF_8));
        } else {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8));
        }
    }

    private String getFieldLabel(String type, String key) {
        return switch (key) {
            case "id" -> type.equals("user") ? "用户ID" : type.equals("resume") ? "简历ID" : type.equals("job") ? "职位ID" : "记录ID";
            case "username" -> "用户名";
            case "email" -> "邮箱";
            case "phone" -> "手机号";
            case "userType" -> "用户类型";
            case "status" -> type.equals("user") ? "状态" : "状态";
            case "createTime" -> "创建时间";
            case "name" -> "姓名";
            case "age" -> "年龄";
            case "education" -> "学历";
            case "skills" -> "技能";
            case "experience", "workExperience", "experienceRequired" -> "工作经验";
            case "expectedSalary" -> "期望薪资";
            case "title", "jobTitle" -> "职位名称";
            case "company" -> "招聘方ID";
            case "location" -> "工作地点";
            case "salaryMin" -> "最低薪资";
            case "salaryMax" -> "最高薪资";
            case "educationRequired" -> "学历要求";
            case "candidateName" -> "候选人ID";
            case "matchScore" -> "匹配分数";
            default -> key;
        };
    }

    private String getUserFieldValue(User user, String key) {
        return switch (key) {
            case "id" -> String.valueOf(user.getId());
            case "username" -> user.getUsername();
            case "email" -> user.getEmail() != null ? user.getEmail() : "";
            case "phone" -> user.getPhone() != null ? user.getPhone() : "";
            case "userType" -> user.getUserType();
            case "status" -> user.getStatus() == 1 ? "正常" : "禁用";
            case "createTime" -> user.getCreateTime() != null ? user.getCreateTime().toString() : "";
            default -> "";
        };
    }

    private String getResumeFieldValue(Resume resume, String key) {
        return switch (key) {
            case "id" -> String.valueOf(resume.getId());
            case "name" -> resume.getName() != null ? resume.getName() : "";
            case "age" -> resume.getAge() != null ? String.valueOf(resume.getAge()) : "";
            case "phone" -> resume.getPhone() != null ? resume.getPhone() : "";
            case "email" -> resume.getEmail() != null ? resume.getEmail() : "";
            case "education" -> resume.getEducation() != null ? resume.getEducation() : "";
            case "skills" -> resume.getSkills() != null ? resume.getSkills() : "";
            case "workExperience", "experience" -> resume.getWorkExperience() != null ? resume.getWorkExperience() : (resume.getExperience() != null ? resume.getExperience() : "");
            case "expectedSalary" -> resume.getExpectedSalary() != null ? resume.getExpectedSalary().toString() : "";
            case "createTime" -> resume.getCreateTime() != null ? resume.getCreateTime().toString() : "";
            default -> "";
        };
    }

    private String getJobFieldValue(Job job, String key) {
        return switch (key) {
            case "id" -> String.valueOf(job.getId());
            case "title" -> job.getTitle() != null ? job.getTitle() : "";
            case "company" -> job.getEmployerId() != null ? String.valueOf(job.getEmployerId()) : "";
            case "location" -> job.getLocation() != null ? job.getLocation() : "";
            case "salaryMin" -> job.getSalaryMin() != null ? job.getSalaryMin().toString() : "";
            case "salaryMax" -> job.getSalaryMax() != null ? job.getSalaryMax().toString() : "";
            case "experienceRequired" -> job.getExperienceRequired() != null ? job.getExperienceRequired() : "";
            case "educationRequired" -> job.getEducationRequired() != null ? job.getEducationRequired() : "";
            case "status" -> job.getStatus() == 1 ? "招聘中" : "已下线";
            case "createTime" -> job.getCreateTime() != null ? job.getCreateTime().toString() : "";
            default -> "";
        };
    }

    private String getMatchRecordFieldValue(MatchRecord record, String key) {
        return switch (key) {
            case "id" -> String.valueOf(record.getId());
            case "resumeId", "candidateName" -> record.getResumeId() != null ? String.valueOf(record.getResumeId()) : "";
            case "jobId", "jobTitle" -> record.getJobId() != null ? String.valueOf(record.getJobId()) : "";
            case "matchScore" -> record.getMatchScore() != null ? record.getMatchScore().toString() : "";
            case "aiSuggestion" -> record.getAiSuggestion() != null ? record.getAiSuggestion() : "";
            case "status" -> {
                int s = record.getStatus() != null ? record.getStatus() : 0;
                yield switch (s) {
                    case 0 -> "待处理";
                    case 1 -> "已查看";
                    case 2 -> "已邀请面试";
                    default -> "未知";
                };
            }
            case "createTime" -> record.getCreateTime() != null ? record.getCreateTime().toString() : "";
            default -> "";
        };
    }
}
