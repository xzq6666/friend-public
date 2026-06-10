package com.smartrecruitment.controller;

import com.smartrecruitment.entity.FavoriteFolder;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.FavoriteFolderService;
import com.smartrecruitment.service.FavoriteService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorite")
@CrossOrigin
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteFolderService favoriteFolderService;

    @Autowired
    private UserService userService;

    /**
     * 添加收藏
     */
    @PostMapping
    public ResponseEntity<?> addFavorite(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            if (body.get("targetType") == null || body.get("targetId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
            }
            Integer targetType = Integer.valueOf(body.get("targetType").toString());
            Long targetId = Long.valueOf(body.get("targetId").toString());

            favoriteService.addFavorite(user.getId(), targetType, targetId);
            return ResponseEntity.ok(Map.of("message", "收藏成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "收藏失败"));
        }
    }

    /**
     * 取消收藏
     */
    @DeleteMapping
    public ResponseEntity<?> removeFavorite(@RequestParam Integer targetType, @RequestParam Long targetId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            favoriteService.removeFavorite(user.getId(), targetType, targetId);
            return ResponseEntity.ok(Map.of("message", "取消收藏成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "取消收藏失败"));
        }
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkFavorited(@RequestParam Integer targetType, @RequestParam Long targetId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            boolean favorited = favoriteService.isFavorited(user.getId(), targetType, targetId);
            return ResponseEntity.ok(Map.of("favorited", favorited));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取收藏的职位
     * @param folderId 文件夹ID，可选。null 或 0 表示未分类，不传表示全部
     */
    @GetMapping("/jobs")
    public ResponseEntity<?> getFavoriteJobs(Authentication auth, @RequestParam(required = false) Long folderId) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            List<Map<String, Object>> jobs = favoriteService.getFavoriteJobs(user.getId(), folderId);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取收藏的简历
     * @param folderId 文件夹ID，可选。null 或 0 表示未分类，不传表示全部
     */
    @GetMapping("/resumes")
    public ResponseEntity<?> getFavoriteResumes(Authentication auth, @RequestParam(required = false) Long folderId) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            List<Map<String, Object>> resumes = favoriteService.getFavoriteResumes(user.getId(), folderId);
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== 文件夹相关接口 ====================

    /**
     * 获取用户的文件夹列表（包含每个文件夹的收藏数量）
     */
    @GetMapping("/folders")
    public ResponseEntity<?> getFolders(Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            // 使用自定义 SQL 查询，LEFT JOIN 统计每个文件夹的收藏数量
            List<Map<String, Object>> folders = favoriteFolderService.getFolderListWithCount(user.getId());
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folders")
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            String name = body.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件夹名称不能为空"));
            }

            FavoriteFolder folder = new FavoriteFolder();
            folder.setUserId(user.getId());
            folder.setName(name.trim());
            favoriteFolderService.save(folder);
            return ResponseEntity.ok(folder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/folders/{id}")
    public ResponseEntity<?> renameFolder(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            FavoriteFolder folder = favoriteFolderService.getById(id);
            if (folder == null) return ResponseEntity.notFound().build();
            if (!folder.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作此文件夹"));
            }

            String name = body.get("name");
            if (name != null && !name.trim().isEmpty()) {
                folder.setName(name.trim());
                favoriteFolderService.updateById(folder);
            }
            return ResponseEntity.ok(folder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            FavoriteFolder folder = favoriteFolderService.getById(id);
            if (folder == null) return ResponseEntity.notFound().build();
            if (!folder.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作此文件夹"));
            }

            // 将该文件夹中的收藏移至未分类（folderId设为null）
            favoriteService.lambdaUpdate()
                    .eq(com.smartrecruitment.entity.Favorite::getFolderId, id)
                    .set(com.smartrecruitment.entity.Favorite::getFolderId, null)
                    .update();

            favoriteFolderService.removeById(id);
            return ResponseEntity.ok(Map.of("message", "文件夹已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 移动收藏到指定文件夹
     * PUT /favorite/{id}/folder
     * body: { folderId: number | null }
     * folderId 为 null 表示移出文件夹（未分类）
     */
    @PutMapping("/{id}/folder")
    public ResponseEntity<?> moveToFolder(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Long folderId = null;
            if (body.get("folderId") != null) {
                folderId = Long.valueOf(body.get("folderId").toString());
            }

            favoriteService.moveToFolder(id, folderId, user.getId());
            return ResponseEntity.ok(Map.of("message", "移动成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "移动失败"));
        }
    }
}
