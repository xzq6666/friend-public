package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.ResumeVersion;
import com.smartrecruitment.mapper.ResumeVersionMapper;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.ResumeVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 简历版本服务实现
 */
@Service
public class ResumeVersionServiceImpl extends ServiceImpl<ResumeVersionMapper, ResumeVersion> 
        implements ResumeVersionService {
    
    private static final Logger log = LoggerFactory.getLogger(ResumeVersionServiceImpl.class);
    
    @Autowired
    private ResumeService resumeService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public ResumeVersion createVersion(Long resumeId, String tag, String note, Long userId) {
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        
        // 获取最新版本号
        String latestVersion = baseMapper.findLatestVersionNumber(resumeId);
        String newVersion = generateNextVersion(latestVersion);
        
        // 创建版本快照
        ResumeVersion version = new ResumeVersion();
        version.setResumeId(resumeId);
        version.setVersionNumber(newVersion);
        version.setVersionTag(tag);
        version.setVersionNote(note);
        version.setCreatedBy(userId);
        
        // 序列化简历数据为JSON
        try {
            version.setSnapshotData(objectMapper.writeValueAsString(resume));
            version.setSkillsSnapshot(resume.getSkills());
            version.setWorkExpSnapshot(resume.getWorkExperience());
        } catch (Exception e) {
            log.error("序列化简历数据失败", e);
            throw new RuntimeException("创建版本失败");
        }
        
        save(version);
        log.info("创建简历版本: resumeId={}, version={}", resumeId, newVersion);
        
        return version;
    }
    
    /**
     * 生成下一个版本号
     */
    private String generateNextVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isEmpty()) {
            return "v1.0";
        }
        
        // 解析版本号 v1.2 -> [1, 2]
        String numPart = currentVersion.replace("v", "");
        String[] parts = numPart.split("\\.");
        
        if (parts.length != 2) {
            return "v1.0";
        }
        
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            
            // 次版本号+1
            return "v" + major + "." + (minor + 1);
        } catch (NumberFormatException e) {
            return "v1.0";
        }
    }
    
    @Override
    public List<ResumeVersion> getVersions(Long resumeId) {
        return baseMapper.findByResumeId(resumeId);
    }
    
    @Override
    public ResumeVersion getVersionDetail(Long versionId) {
        ResumeVersion version = getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        return version;
    }
    
    @Override
    @Transactional
    public boolean restoreToVersion(Long versionId, Long userId) {
        ResumeVersion version = getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        
        // 验证权限（简化版：实际应该检查userId）
        
        try {
            // 反序列化快照数据
            Resume snapshot = objectMapper.readValue(version.getSnapshotData(), Resume.class);
            
            // 获取当前简历
            Resume currentResume = resumeService.getById(version.getResumeId());
            if (currentResume == null) {
                throw new IllegalArgumentException("简历不存在");
            }
            
            // 恢复关键字段
            currentResume.setName(snapshot.getName());
            currentResume.setAge(snapshot.getAge());
            currentResume.setEducation(snapshot.getEducation());
            currentResume.setExperience(snapshot.getExperience());
            currentResume.setExpectedSalary(snapshot.getExpectedSalary());
            currentResume.setSkills(snapshot.getSkills());
            currentResume.setWorkExperience(snapshot.getWorkExperience());
            currentResume.setSelfIntroduction(snapshot.getSelfIntroduction());
            currentResume.setCategoryId(snapshot.getCategoryId());
            
            // 更新简历
            resumeService.updateById(currentResume);
            
            log.info("恢复简历版本: resumeId={}, version={}", version.getResumeId(), version.getVersionNumber());
            return true;
            
        } catch (Exception e) {
            log.error("恢复版本失败", e);
            throw new RuntimeException("恢复版本失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> compareVersions(Long version1Id, Long version2Id) {
        ResumeVersion v1 = getById(version1Id);
        ResumeVersion v2 = getById(version2Id);
        
        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        
        if (!v1.getResumeId().equals(v2.getResumeId())) {
            throw new IllegalArgumentException("只能对比同一简历的版本");
        }
        
        Map<String, Object> comparison = new HashMap<>();
        
        try {
            Resume r1 = objectMapper.readValue(v1.getSnapshotData(), Resume.class);
            Resume r2 = objectMapper.readValue(v2.getSnapshotData(), Resume.class);
            
            // 对比各个字段
            Map<String, Object> changes = new HashMap<>();
            
            // 基本信息对比
            changes.put("name", compareField(r1.getName(), r2.getName()));
            changes.put("age", compareField(r1.getAge(), r2.getAge()));
            changes.put("education", compareField(r1.getEducation(), r2.getEducation()));
            changes.put("experience", compareField(r1.getExperience(), r2.getExperience()));
            changes.put("expectedSalary", compareField(r1.getExpectedSalary(), r2.getExpectedSalary()));
            
            // 技能对比
            changes.put("skills", compareSkills(r1.getSkills(), r2.getSkills()));
            
            // 工作经历对比
            changes.put("workExperience", compareWorkExperience(r1.getWorkExperience(), r2.getWorkExperience()));
            
            // 自我评价对比
            changes.put("selfIntroduction", compareField(r1.getSelfIntroduction(), r2.getSelfIntroduction()));
            
            comparison.put("changes", changes);
            comparison.put("version1", v1.getVersionNumber());
            comparison.put("version2", v2.getVersionNumber());
            comparison.put("timeDiff", calculateTimeDiff(v1.getCreateTime(), v2.getCreateTime()));
            
        } catch (Exception e) {
            log.error("对比版本失败", e);
            throw new RuntimeException("对比版本失败");
        }
        
        return comparison;
    }
    
    /**
     * 对比单个字段
     */
    private Map<String, Object> compareField(Object oldVal, Object newVal) {
        Map<String, Object> result = new HashMap<>();
        result.put("old", oldVal);
        result.put("new", newVal);
        result.put("changed", !Objects.equals(oldVal, newVal));
        return result;
    }
    
    /**
     * 对比技能
     */
    private Map<String, Object> compareSkills(String oldSkills, String newSkills) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Set<String> oldSet = parseSkills(oldSkills);
            Set<String> newSet = parseSkills(newSkills);
            
            // 新增的技能
            Set<String> added = new HashSet<>(newSet);
            added.removeAll(oldSet);
            
            // 删除的技能
            Set<String> removed = new HashSet<>(oldSet);
            removed.removeAll(newSet);
            
            result.put("added", added);
            result.put("removed", removed);
            result.put("unchanged", oldSet.stream().filter(newSet::contains).toArray());
            result.put("changed", !added.isEmpty() || !removed.isEmpty());
            
        } catch (Exception e) {
            result.put("error", "解析技能失败");
        }
        
        return result;
    }
    
    /**
     * 解析技能JSON
     */
    private Set<String> parseSkills(String skillsJson) {
        if (skillsJson == null || skillsJson.isEmpty()) {
            return new HashSet<>();
        }
        try {
            List<String> skills = objectMapper.readValue(skillsJson, List.class);
            return new HashSet<>(skills);
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
    
    /**
     * 对比工作经历（简化版）
     */
    private Map<String, Object> compareWorkExperience(String oldExp, String newExp) {
        Map<String, Object> result = new HashMap<>();
        result.put("old", oldExp);
        result.put("new", newExp);
        result.put("changed", !Objects.equals(oldExp, newExp));
        return result;
    }
    
    /**
     * 计算时间差
     */
    private String calculateTimeDiff(java.time.LocalDateTime t1, java.time.LocalDateTime t2) {
        if (t1 == null || t2 == null) {
            return "未知";
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(t1.isBefore(t2) ? t1 : t2, t1.isBefore(t2) ? t2 : t1);
        if (days == 0) {
            return "同一天";
        } else if (days < 7) {
            return days + "天";
        } else if (days < 30) {
            return (days / 7) + "周";
        } else {
            return (days / 30) + "个月";
        }
    }
    
    @Override
    @Transactional
    public boolean deleteVersion(Long versionId, Long userId) {
        ResumeVersion version = getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        
        // TODO: 权限检查
        
        return removeById(versionId);
    }
    
    @Override
    public String getLatestVersionNumber(Long resumeId) {
        String version = baseMapper.findLatestVersionNumber(resumeId);
        return version != null ? version : "v1.0";
    }
}
