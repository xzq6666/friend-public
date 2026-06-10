package com.smartrecruitment.service;

import com.smartrecruitment.entity.Favorite;
import com.smartrecruitment.mapper.FavoriteMapper;
import com.smartrecruitment.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FavoriteService 单元测试
 * 测试收藏职位/简历、取消收藏、查询收藏状态等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @Mock
    private FavoriteMapper favoriteMapper;

    private Favorite testFavorite;

    @BeforeEach
    void setUp() {
        testFavorite = new Favorite();
        testFavorite.setId(1L);
        testFavorite.setUserId(100L);
        testFavorite.setTargetType(1); // 1-职位
        testFavorite.setTargetId(1L);
    }

    // ======================== 添加收藏测试 ========================

    @Test
    void testAddFavorite_success() {
        // 模拟未收藏
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.insert(any(Favorite.class))).thenReturn(1);

        Favorite result = favoriteService.addFavorite(100L, 1, 1L);

        assertNotNull(result, "添加收藏应返回收藏记录");
        assertEquals(100L, result.getUserId());
        assertEquals(1, result.getTargetType());
        assertEquals(1L, result.getTargetId());
    }

    @Test
    void testAddFavorite_alreadyFavorited_shouldThrowException() {
        // 模拟已收藏
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            favoriteService.addFavorite(100L, 1, 1L);
        });
        assertEquals("已经收藏过了", ex.getMessage());
    }

    // ======================== 取消收藏测试 ========================

    @Test
    void testRemoveFavorite_success() {
        when(favoriteMapper.delete(any())).thenReturn(1);

        boolean result = favoriteService.removeFavorite(100L, 1, 1L);

        assertTrue(result, "取消收藏应返回 true");
    }

    @Test
    void testRemoveFavorite_notFavorited_shouldReturnFalse() {
        when(favoriteMapper.delete(any())).thenReturn(0);

        boolean result = favoriteService.removeFavorite(100L, 1, 999L);

        assertFalse(result, "未收藏时取消应返回 false");
    }

    // ======================== 查询收藏状态测试 ========================

    @Test
    void testIsFavorited_true() {
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        boolean result = favoriteService.isFavorited(100L, 1, 1L);

        assertTrue(result, "已收藏应返回 true");
    }

    @Test
    void testIsFavorited_false() {
        when(favoriteMapper.selectCount(any())).thenReturn(0L);

        boolean result = favoriteService.isFavorited(100L, 1, 999L);

        assertFalse(result, "未收藏应返回 false");
    }

    // ======================== 移动到文件夹测试 ========================

    @Test
    void testMoveToFolder_success() {
        when(favoriteMapper.selectById(1L)).thenReturn(testFavorite);
        when(favoriteMapper.updateById(any(Favorite.class))).thenReturn(1);

        boolean result = favoriteService.moveToFolder(1L, 5L, 100L);

        assertTrue(result, "移动到文件夹应成功");
        assertEquals(5L, testFavorite.getFolderId());
    }

    @Test
    void testMoveToFolder_notOwner_shouldThrowException() {
        when(favoriteMapper.selectById(1L)).thenReturn(testFavorite);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            favoriteService.moveToFolder(1L, 5L, 999L); // 非所有者
        });
        assertEquals("无权操作此收藏记录", ex.getMessage());
    }

    @Test
    void testMoveToFolder_notExists_shouldThrowException() {
        when(favoriteMapper.selectById(999L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            favoriteService.moveToFolder(999L, 5L, 100L);
        });
        assertEquals("收藏记录不存在", ex.getMessage());
    }

    @Test
    void testMoveToFolder_nullFolderId_shouldMoveOut() {
        when(favoriteMapper.selectById(1L)).thenReturn(testFavorite);
        when(favoriteMapper.updateById(any(Favorite.class))).thenReturn(1);

        boolean result = favoriteService.moveToFolder(1L, null, 100L);

        assertTrue(result, "移出文件夹应成功");
        assertNull(testFavorite.getFolderId(), "folderId 应为 null");
    }
}
