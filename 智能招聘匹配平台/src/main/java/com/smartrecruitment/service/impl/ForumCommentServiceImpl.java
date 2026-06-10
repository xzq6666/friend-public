package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.ForumComment;
import com.smartrecruitment.entity.ForumPost;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.ForumCommentMapper;
import com.smartrecruitment.mapper.ForumPostMapper;
import com.smartrecruitment.mapper.JobMapper;
import com.smartrecruitment.mapper.UserMapper;
import com.smartrecruitment.service.ForumCommentService;
import com.smartrecruitment.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ForumCommentServiceImpl extends ServiceImpl<ForumCommentMapper, ForumComment> implements ForumCommentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ForumPostMapper postMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    public IPage<ForumComment> getCommentList(Long postId, int page, int size) {
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getPostId, postId)
                .eq(ForumComment::getStatus, 1)
                .orderByAsc(ForumComment::getCreateTime);

        IPage<ForumComment> commentPage = page(new Page<>(page, size), wrapper);
        commentPage.getRecords().forEach(this::fillUserInfo);
        return commentPage;
    }

    @Override
    public List<ForumComment> getCommentTree(Long postId) {
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getPostId, postId)
                .eq(ForumComment::getStatus, 1)
                .orderByAsc(ForumComment::getCreateTime);

        List<ForumComment> allComments = list(wrapper);
        allComments.forEach(this::fillUserInfo);

        // 构建树形结构
        Map<Long, List<ForumComment>> parentMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(ForumComment::getParentId));

        List<ForumComment> rootComments = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        rootComments.forEach(root -> buildChildren(root, parentMap));

        return rootComments;
    }

    private void buildChildren(ForumComment parent, Map<Long, List<ForumComment>> parentMap) {
        List<ForumComment> children = parentMap.getOrDefault(parent.getId(), new ArrayList<>());
        parent.setChildren(children);
        children.forEach(child -> buildChildren(child, parentMap));
    }

    @Override
    @Transactional
    public ForumComment createComment(ForumComment comment) {
        // 检查帖子是否存在且未关闭
        ForumPost post = postMapper.selectById(comment.getPostId());
        if (post == null || post.getStatus() == 0) {
            return null;
        }
        if (post.getIsClosed() == 1) {
            return null; // 帖子已关闭，不允许评论
        }

        comment.setStatus(1);
        save(comment);

        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        // 回复通知：被回复的用户收到站内通知
        if (comment.getReplyToUserId() != null && !comment.getReplyToUserId().equals(comment.getUserId())) {
            try {
                User replier = userMapper.selectById(comment.getUserId());
                String replierName = replier != null ? replier.getUsername() : "某用户";
                String snippet = comment.getContent().length() > 50
                        ? comment.getContent().substring(0, 50) + "..."
                        : comment.getContent();
                notificationService.send(
                        comment.getReplyToUserId(),
                        "收到新回复",
                        String.format("用户 %s 回复了你的评论：%s\njobId:%d", replierName, snippet, post.getJobId()),
                        "system"
                );
            } catch (Exception e) {
                // 通知失败不影响评论主流程
            }
        }

        // 帖子作者通知（非自己发的评论才通知）
        if (!post.getUserId().equals(comment.getUserId())
                && (comment.getReplyToUserId() == null || !comment.getReplyToUserId().equals(post.getUserId()))) {
            try {
                User commenter = userMapper.selectById(comment.getUserId());
                String commenterName = commenter != null ? commenter.getUsername() : "某用户";
                notificationService.send(
                        post.getUserId(),
                        "帖子有新评论",
                        String.format("用户 %s 评论了你的帖子 [%s]\njobId:%d", commenterName, post.getTitle(), post.getJobId()),
                        "system"
                );
            } catch (Exception e) {
                // 通知失败不影响评论主流程
            }
        }

        // 企业通知：评论者不是企业方时，通知对应企业
        if (post.getJobId() != null) {
            try {
                Job job = jobMapper.selectById(post.getJobId());
                if (job != null && job.getEmployerId() != null
                        && !job.getEmployerId().equals(comment.getUserId())) {
                    // 避免与帖子作者通知重复
                    if (!job.getEmployerId().equals(post.getUserId())) {
                        User commenter = userMapper.selectById(comment.getUserId());
                        String commenterName = commenter != null ? commenter.getUsername() : "某用户";
                        notificationService.send(
                                job.getEmployerId(),
                                "职位论坛有新评论",
                                String.format("用户 %s 在职位 [%s] 的论坛发表了评论\njobId:%d", commenterName, job.getTitle(), post.getJobId()),
                                "system"
                        );
                    }
                }
            } catch (Exception e) {
                // 通知失败不影响评论主流程
            }
        }

        return getById(comment.getId());
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId, String userType) {
        ForumComment comment = getById(commentId);
        if (comment == null) {
            return false;
        }

        // 权限检查：评论人、职位所属企业、管理员可以删除
        if (!comment.getUserId().equals(userId) && !"ADMIN".equals(userType)) {
            if ("EMPLOYER".equals(userType)) {
                ForumPost post = postMapper.selectById(comment.getPostId());
                if (post != null) {
                    Job job = jobMapper.selectById(post.getJobId());
                    if (job == null || !job.getEmployerId().equals(userId)) {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        Long postId = comment.getPostId();

        // 软删除
        comment.setStatus(0);
        boolean success = updateById(comment);

        // 重新计算帖子评论数（查询实际的有效评论数）
        if (success) {
            Long count = count(new LambdaQueryWrapper<ForumComment>()
                    .eq(ForumComment::getPostId, postId)
                    .eq(ForumComment::getStatus, 1));
            ForumPost post = postMapper.selectById(postId);
            if (post != null) {
                post.setCommentCount(count.intValue());
                postMapper.updateById(post);
            }
        }

        return success;
    }

    private void fillUserInfo(ForumComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setAvatar(user.getAvatar());
            comment.setUserType(user.getUserType());
        }
        if (comment.getReplyToUserId() != null) {
            User replyToUser = userMapper.selectById(comment.getReplyToUserId());
            if (replyToUser != null) {
                comment.setReplyToUsername(replyToUser.getUsername());
            }
        }
    }
}
