package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Announcement;

public interface AnnouncementService extends IService<Announcement> {
    Announcement publish(Announcement announcement);
}
