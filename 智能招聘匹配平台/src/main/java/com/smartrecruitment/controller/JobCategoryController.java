package com.smartrecruitment.controller;

import com.smartrecruitment.entity.JobCategory;
import com.smartrecruitment.service.JobCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
public class JobCategoryController {

    @Autowired
    private JobCategoryService jobCategoryService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<JobCategory> categories = jobCategoryService.getAllCategories();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", categories);
        return result;
    }
}
