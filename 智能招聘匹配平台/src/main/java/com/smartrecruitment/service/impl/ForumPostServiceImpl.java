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
import com.smartrecruitment.service.ForumPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements ForumPostService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private ForumCommentMapper commentMapper;

    @Override
    public IPage<ForumPost> getPostList(Long jobId, int page, int size) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getJobId, jobId)
                .eq(ForumPost::getStatus, 1)
                .orderByDesc(ForumPost::getIsPinned)
                .orderByDesc(ForumPost::getCreateTime);

        IPage<ForumPost> postPage = page(new Page<>(page, size), wrapper);

        // 填充发帖人信息
        postPage.getRecords().forEach(this::fillUserInfo);

        return postPage;
    }

    @Override
    public ForumPost getPostDetail(Long postId) {
        ForumPost post = getById(postId);
        if (post == null || post.getStatus() == 0) {
            return null;
        }
        fillUserInfo(post);
        return post;
    }

    @Override
    @Transactional
    public ForumPost createPost(ForumPost post) {
        post.setStatus(1);
        post.setIsPinned(0);
        post.setIsClosed(0);
        post.setCommentCount(0);
        save(post);
        return getById(post.getId());
    }

    @Override
    public boolean updatePost(ForumPost post, Long userId) {
        ForumPost existing = getById(post.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            return false;
        }
        existing.setTitle(post.getTitle());
        existing.setContent(post.getContent());
        return updateById(existing);
    }

    @Override
    public boolean deletePost(Long postId, Long userId, String userType) {
        ForumPost post = getById(postId);
        if (post == null) {
            return false;
        }

        // 权限检查：发帖人、职位所属企业、管理员可以删除
        if (!post.getUserId().equals(userId) && !"ADMIN".equals(userType)) {
            if ("EMPLOYER".equals(userType)) {
                Job job = jobMapper.selectById(post.getJobId());
                if (job == null || !job.getEmployerId().equals(userId)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        // 软删除
        post.setStatus(0);
        return updateById(post);
    }

    @Override
    public boolean togglePin(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (post == null) {
            return false;
        }

        // 只有职位所属企业可以置顶
        Job job = jobMapper.selectById(post.getJobId());
        if (job == null || !job.getEmployerId().equals(userId)) {
            return false;
        }

        post.setIsPinned(post.getIsPinned() == 1 ? 0 : 1);
        return updateById(post);
    }

    @Override
    public boolean toggleClose(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (post == null) {
            return false;
        }

        // 只有职位所属企业可以关闭讨论
        Job job = jobMapper.selectById(post.getJobId());
        if (job == null || !job.getEmployerId().equals(userId)) {
            return false;
        }

        post.setIsClosed(post.getIsClosed() == 1 ? 0 : 1);
        return updateById(post);
    }

    private void fillUserInfo(ForumPost post) {
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            post.setUsername(user.getUsername());
            post.setAvatar(user.getAvatar());
            post.setUserType(user.getUserType());
        }
    }
}
