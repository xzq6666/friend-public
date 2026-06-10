package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Announcement;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.AnnouncementMapper;
import com.smartrecruitment.service.AnnouncementService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Override
    public Announcement publish(Announcement announcement) {
        save(announcement);

        // 根据 targetType 查询目标用户，批量创建通知
        List<Long> targetIds;
        String targetType = announcement.getTargetType();
        if ("ALL".equals(targetType)) {
            targetIds = userService.list().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        } else {
            targetIds = userService.lambdaQuery()
                    .eq(User::getUserType, targetType)
                    .list().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        // 排除发布者自己
        targetIds.removeIf(id -> id.equals(announcement.getPublisherId()));

        notificationService.sendBatch(targetIds, "系统公告", announcement.getTitle(), "announcement");

        return announcement;
    }
}
