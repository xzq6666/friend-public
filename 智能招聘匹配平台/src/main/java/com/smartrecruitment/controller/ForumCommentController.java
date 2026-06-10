package com.smartrecruitment.controller;

import com.smartrecruitment.entity.ForumComment;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.ForumCommentService;
import com.smartrecruitment.service.SensitiveWordService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/forum/comment")
@CrossOrigin
public class ForumCommentController {

    @Autowired
    private ForumCommentService forumCommentService;

    @Autowired
    private UserService userService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /**
     * 获取帖子评论（树形结构）
     */
    @GetMapping("/tree")
    public ResponseEntity<?> getCommentTree(@RequestParam Long postId) {
        List<ForumComment> tree = forumCommentService.getCommentTree(postId);
        return ResponseEntity.ok(tree);
    }

    /**
     * 获取单个评论详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getComment(@PathVariable Long id) {
        ForumComment comment = forumCommentService.getById(id);
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(comment);
    }

    /**
     * 发表评论
     */
    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody ForumComment comment, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "评论内容不能为空"));
        }
        if (comment.getPostId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "帖子ID不能为空"));
        }

        // 敏感词检测
        List<String> found = sensitiveWordService.checkSensitiveWordsWithAi(comment.getContent());
        if (!found.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "评论内容包含敏感词: " + String.join(", ", found),
                    "sensitiveWords", found
            ));
        }

        comment.setUserId(currentUser.getId());
        ForumComment created = forumCommentService.createComment(comment);
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "评论失败，帖子可能已关闭或不存在"));
        }
        return ResponseEntity.ok(Map.of("message", "评论成功", "commentId", created.getId()));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        boolean success = forumCommentService.deleteComment(id, currentUser.getId(), currentUser.getUserType());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "删除失败，可能没有权限"));
    }
}
