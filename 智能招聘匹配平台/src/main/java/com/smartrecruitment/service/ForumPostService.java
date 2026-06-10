package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.ForumPost;

public interface ForumPostService extends IService<ForumPost> {

    IPage<ForumPost> getPostList(Long jobId, int page, int size);

    ForumPost getPostDetail(Long postId);

    ForumPost createPost(ForumPost post);

    boolean updatePost(ForumPost post, Long userId);

    boolean deletePost(Long postId, Long userId, String userType);

    boolean togglePin(Long postId, Long userId);

    boolean toggleClose(Long postId, Long userId);
}
