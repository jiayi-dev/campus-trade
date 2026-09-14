package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/batch")
    public String batch(
            @RequestBody List<FavoriteService.FavoriteItem> list) {

        try {
            favoriteService.batchProcess(list);
            return "批量收藏处理成功";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}