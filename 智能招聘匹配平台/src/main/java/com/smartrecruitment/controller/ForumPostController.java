package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartrecruitment.entity.ForumPost;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.ForumPostService;
import com.smartrecruitment.service.SensitiveWordService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/forum/post")
@CrossOrigin
public class ForumPostController {

    @Autowired
    private ForumPostService forumPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /**
     * 获取职位帖子列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getPostList(
            @RequestParam Long jobId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<ForumPost> postPage = forumPostService.getPostList(jobId, page, size);
        return ResponseEntity.ok(postPage);
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long id) {
        ForumPost post = forumPostService.getPostDetail(id);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    /**
     * 发帖
     */
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody ForumPost post, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "帖子标题不能为空"));
        }
        if (post.getJobId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "职位ID不能为空"));
        }

        // 敏感词检测
        String textToCheck = (post.getTitle() != null ? post.getTitle() : "") + " "
                + (post.getContent() != null ? post.getContent() : "");
        List<String> found = sensitiveWordService.checkSensitiveWordsWithAi(textToCheck);
        if (!found.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "帖子内容包含敏感词: " + String.join(", ", found),
                    "sensitiveWords", found
            ));
        }

        post.setUserId(currentUser.getId());
        ForumPost created = forumPostService.createPost(post);
        return ResponseEntity.ok(Map.of("message", "发帖成功", "postId", created.getId()));
    }

    /**
     * 编辑帖子
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody ForumPost post, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        // 敏感词检测
        String textToCheck = (post.getTitle() != null ? post.getTitle() : "") + " "
                + (post.getContent() != null ? post.getContent() : "");
        if (!textToCheck.trim().isEmpty()) {
            List<String> found = sensitiveWordService.checkSensitiveWordsWithAi(textToCheck);
            if (!found.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "帖子内容包含敏感词: " + String.join(", ", found),
                        "sensitiveWords", found
                ));
            }
        }

        post.setId(id);
        boolean success = forumPostService.updatePost(post, currentUser.getId());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "编辑成功"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "编辑失败，可能没有权限"));
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        boolean success = forumPostService.deletePost(id, currentUser.getId(), currentUser.getUserType());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "删除失败，可能没有权限"));
    }

    /**
     * 置顶/取消置顶
     */
    @PutMapping("/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        boolean success = forumPostService.togglePin(id, currentUser.getId());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "操作成功"));
        }
        return ResponseEntity.status(403).body(Map.of("error", "没有权限，仅职位所属企业可操作"));
    }

    /**
     * 关闭/开启讨论
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<?> toggleClose(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        boolean success = forumPostService.toggleClose(id, currentUser.getId());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "操作成功"));
        }
        return ResponseEntity.status(403).body(Map.of("error", "没有权限，仅职位所属企业可操作"));
    }
}
