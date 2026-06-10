package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartrecruitment.entity.SensitiveWord;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.SensitiveWordService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sensitive-word")
@CrossOrigin
public class SensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private UserService userService;

    private User checkAdmin(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) return null;
        User user = userService.findByUsername(auth.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) return null;
        return user;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        IPage<SensitiveWord> result = sensitiveWordService.getWordList(keyword, category, source, status, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        return ResponseEntity.ok(sensitiveWordService.getStats());
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody SensitiveWord word, Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        if (word.getWord() == null || word.getWord().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "敏感词不能为空"));
        }
        if (word.getWord().trim().length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "敏感词长度不能少于2个字符"));
        }
        word.setWord(word.getWord().trim());
        SensitiveWord added = sensitiveWordService.addWord(word);
        if (added == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "该敏感词已存在"));
        }
        return ResponseEntity.ok(added);
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchAdd(@RequestBody Map<String, Object> body, Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        String text = (String) body.get("words");
        String category = (String) body.get("category");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入敏感词"));
        }
        int added = sensitiveWordService.batchAddWords(text, category);
        return ResponseEntity.ok(Map.of("message", "成功添加 " + added + " 个敏感词", "added", added));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SensitiveWord word, Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        word.setId(id);
        boolean ok = sensitiveWordService.updateWord(word);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("error", "更新失败"));
        }
        return ResponseEntity.ok(Map.of("message", "更新成功"));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody Map<String, List<Long>> body, Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择要删除的记录"));
        }
        boolean ok = sensitiveWordService.deleteWords(ids);
        return ok ? ResponseEntity.ok(Map.of("message", "删除成功"))
                : ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id, Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        boolean ok = sensitiveWordService.toggleStatus(id);
        return ok ? ResponseEntity.ok(Map.of("message", "状态切换成功"))
                : ResponseEntity.badRequest().body(Map.of("error", "操作失败"));
    }

    @PutMapping("/batch-toggle")
    public ResponseEntity<?> batchToggle(@RequestBody Map<String, Object> body, Authentication auth) {
        if (checkAdmin(auth) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) body.get("ids")).stream().map(Number::longValue).collect(Collectors.toList());
        Integer status = (Integer) body.get("status");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择要操作的记录"));
        }
        if (status == null || (status != 0 && status != 1)) {
            return ResponseEntity.badRequest().body(Map.of("error", "状态值无效"));
        }
        int count = sensitiveWordService.batchToggleWords(ids, status);
        return ResponseEntity.ok(Map.of("message", "成功更新 " + count + " 条记录", "updated", count));
    }

    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody Map<String, String> body, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        String text = body.get("text");
        if (text == null || text.isEmpty()) {
            return ResponseEntity.ok(Map.of("sensitiveWords", List.of()));
        }
        List<String> found = sensitiveWordService.checkSensitiveWords(text);
        return ResponseEntity.ok(Map.of("sensitiveWords", found, "hasSensitive", !found.isEmpty()));
    }
}
