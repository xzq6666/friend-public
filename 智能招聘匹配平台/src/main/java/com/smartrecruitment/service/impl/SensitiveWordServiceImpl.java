package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.config.BailianConfig;
import com.smartrecruitment.entity.SensitiveWord;
import com.smartrecruitment.mapper.SensitiveWordMapper;
import com.smartrecruitment.service.SensitiveWordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordServiceImpl.class);

    @Autowired
    private BailianConfig bailianConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 内存缓存：所有启用的敏感词
    private final CopyOnWriteArraySet<String> enabledWords = new CopyOnWriteArraySet<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    private void refreshCache() {
        try {
            List<SensitiveWord> words = lambdaQuery()
                    .eq(SensitiveWord::getStatus, 1)
                    .select(SensitiveWord::getWord)
                    .list();
            enabledWords.clear();
            words.forEach(w -> {
                if (w.getWord() != null && !w.getWord().isEmpty()) {
                    enabledWords.add(w.getWord().toLowerCase());
                }
            });
            log.info("敏感词缓存已刷新，共 {} 个启用词", enabledWords.size());
        } catch (Exception e) {
            log.error("刷新敏感词缓存失败", e);
        }
    }

    @Override
    public IPage<SensitiveWord> getWordList(String keyword, String category, String source, Integer status, int page, int size) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SensitiveWord::getWord, keyword);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(SensitiveWord::getCategory, category);
        }
        if (source != null && !source.isEmpty()) {
            wrapper.eq(SensitiveWord::getSource, source);
        }
        if (status != null) {
            wrapper.eq(SensitiveWord::getStatus, status);
        }
        wrapper.orderByDesc(SensitiveWord::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public SensitiveWord addWord(SensitiveWord word) {
        // 检查重复
        Long count = lambdaQuery().eq(SensitiveWord::getWord, word.getWord()).count();
        if (count > 0) {
            return null; // 已存在
        }
        save(word);
        if (word.getStatus() != null && word.getStatus() == 1) {
            enabledWords.add(word.getWord().toLowerCase());
        }
        return word;
    }

    @Override
    public int batchAddWords(String text, String category) {
        if (text == null || text.isEmpty()) return 0;
        // 按逗号、分号、换行、空格分割
        String[] parts = text.split("[,，;；\\n\\r\\s]+");
        int added = 0;
        for (String part : parts) {
            String word = part.trim();
            if (word.isEmpty() || word.length() < 2) continue;
            Long count = lambdaQuery().eq(SensitiveWord::getWord, word).count();
            if (count > 0) continue;
            SensitiveWord sw = new SensitiveWord();
            sw.setWord(word);
            sw.setCategory(category != null ? category : "自定义");
            sw.setSource("manual");
            sw.setStatus(1);
            save(sw);
            enabledWords.add(word.toLowerCase());
            added++;
        }
        return added;
    }

    @Override
    public boolean updateWord(SensitiveWord word) {
        boolean ok = updateById(word);
        if (ok) refreshCache();
        return ok;
    }

    @Override
    public boolean deleteWords(List<Long> ids) {
        boolean ok = removeByIds(ids);
        if (ok) refreshCache();
        return ok;
    }

    @Override
    public boolean toggleStatus(Long id) {
        SensitiveWord word = getById(id);
        if (word == null) return false;
        word.setStatus(word.getStatus() == 1 ? 0 : 1);
        boolean ok = updateById(word);
        if (ok) refreshCache();
        return ok;
    }

    @Override
    public int batchToggleWords(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return 0;
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SensitiveWord::getId, ids);
        SensitiveWord update = new SensitiveWord();
        update.setStatus(status);
        boolean ok = update(update, wrapper);
        if (ok) refreshCache();
        return ok ? ids.size() : 0;
    }

    @Override
    public List<String> checkSensitiveWords(String text) {
        if (text == null || text.isEmpty() || enabledWords.isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = normalizeText(text).toLowerCase();
        List<String> found = new ArrayList<>();
        for (String word : enabledWords) {
            if (normalized.contains(word)) {
                found.add(word);
            }
        }
        // 对原始文本也做一次精确匹配
        String lowerText = text.toLowerCase();
        for (String word : enabledWords) {
            if (!found.contains(word) && lowerText.contains(word)) {
                found.add(word);
            }
        }
        return found;
    }

    @Override
    public List<String> checkSensitiveWordsWithAi(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        // 第一层：字典精确匹配
        List<String> found = checkSensitiveWords(text);
        if (!found.isEmpty()) return found;
        // 第二层：AI智能检测
        try {
            List<String> aiFound = aiDetectSensitiveWords(text);
            if (aiFound != null && !aiFound.isEmpty()) {
                // AI检测到的新词自动入库（待审核状态）
                autoSaveAiDetectedWords(aiFound);
                return aiFound;
            }
        } catch (Exception e) {
            log.warn("AI敏感词检测异常，跳过: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> aiDetectSensitiveWords(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        try {
            String prompt = "请分析以下文本，识别其中可能存在的敏感词（包括但不限于：政治敏感、色情低俗、暴力血腥、广告推销、人身攻击、歧视性用语、赌博、毒品、违法信息）。\n"
                    + "请注意识别谐音、拼音首字母、特殊符号替代等变体写法。\n"
                    + "请以纯JSON数组格式返回检测到的敏感词列表，例如 [\"敏感词1\", \"敏感词2\"]。如果没有敏感词，返回空数组 []。\n"
                    + "只返回JSON数组，不要返回其他任何内容。\n\n"
                    + "文本：" + text;
            String response = bailianConfig.call(prompt);
            if (response == null || response.isEmpty() || response.equals("{}")) {
                return Collections.emptyList();
            }
            // 提取JSON数组
            String jsonStr = response.trim();
            int start = jsonStr.indexOf('[');
            int end = jsonStr.lastIndexOf(']');
            if (start != -1 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }
            JsonNode array = objectMapper.readTree(jsonStr);
            List<String> words = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String w = node.asText("").trim();
                    if (!w.isEmpty() && w.length() >= 2) {
                        words.add(w);
                    }
                }
            }
            return words;
        } catch (Exception e) {
            log.warn("AI敏感词检测失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void autoSaveAiDetectedWords(List<String> words) {
        if (words == null || words.isEmpty()) return;
        for (String word : words) {
            Long count = lambdaQuery().eq(SensitiveWord::getWord, word).count();
            if (count > 0) continue;
            SensitiveWord sw = new SensitiveWord();
            sw.setWord(word);
            sw.setCategory("AI检测");
            sw.setSource("ai");
            sw.setStatus(0); // 待审核
            save(sw);
            log.info("AI自动入库敏感词: {}", word);
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", count());
        stats.put("enabled", lambdaQuery().eq(SensitiveWord::getStatus, 1).count());
        stats.put("disabled", lambdaQuery().eq(SensitiveWord::getStatus, 0).count());
        stats.put("aiDetected", lambdaQuery().eq(SensitiveWord::getSource, "ai").count());
        stats.put("manual", lambdaQuery().eq(SensitiveWord::getSource, "manual").count());
        return stats;
    }

    /**
     * 文本标准化：去除干扰字符，处理变体写法
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        // 全角转半角
        for (char c : text.toCharArray()) {
            if (c >= 0xFF01 && c <= 0xFF5E) {
                sb.append((char) (c - 0xFEE0));
            } else if (c == 0x3000) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        String result = sb.toString();
        // 去除中文字符间的空格和干扰符号
        result = result.replaceAll("(?<=[\\u4e00-\\u9fa5])\\s+(?=[\\u4e00-\\u9fa5])", "");
        // 去除常见干扰符号
        result = result.replaceAll("[*#._\\-~]", "");
        // 常见符号替代映射
        result = result.replace("@", "a")
                .replace("$", "s")
                .replace("0", "o")
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("5", "s")
                .replace("7", "t")
                .replace("|", "l");
        return result;
    }
}
