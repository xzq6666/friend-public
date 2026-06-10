package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.ForumComment;

import java.util.List;

public interface ForumCommentService extends IService<ForumComment> {

    IPage<ForumComment> getCommentList(Long postId, int page, int size);

    List<ForumComment> getCommentTree(Long postId);

    ForumComment createComment(ForumComment comment);

    boolean deleteComment(Long commentId, Long userId, String userType);
}
