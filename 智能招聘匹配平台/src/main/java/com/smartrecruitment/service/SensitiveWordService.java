package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.SensitiveWord;

import java.util.List;
import java.util.Map;

public interface SensitiveWordService extends IService<SensitiveWord> {

    IPage<SensitiveWord> getWordList(String keyword, String category, String source, Integer status, int page, int size);

    SensitiveWord addWord(SensitiveWord word);

    int batchAddWords(String text, String category);

    boolean updateWord(SensitiveWord word);

    boolean deleteWords(List<Long> ids);

    boolean toggleStatus(Long id);

    int batchToggleWords(List<Long> ids, Integer status);

    List<String> checkSensitiveWords(String text);

    /**
     * 综合检测：先字典匹配，未命中则调用AI检测（自动入库新词）
     */
    List<String> checkSensitiveWordsWithAi(String text);

    List<String> aiDetectSensitiveWords(String text);

    void autoSaveAiDetectedWords(List<String> words);

    Map<String, Object> getStats();
}
