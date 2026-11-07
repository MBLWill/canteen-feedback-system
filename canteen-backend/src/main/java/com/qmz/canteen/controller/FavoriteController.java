package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.FavoriteService;
import com.qmz.canteen.vo.DishVO;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<List<DishVO>> list() {
        return Result.success(favoriteService.listMyFavorites(UserContext.getUserId()));
    }

    @PostMapping("/{dishId}")
    public Result<Map<String, Boolean>> add(@PathVariable @Positive(message = "菜品ID不合法") Long dishId) {
        return Result.success(Map.of("added", favoriteService.add(dishId, UserContext.getUserId())));
    }

    @DeleteMapping("/{dishId}")
    public Result<Void> remove(@PathVariable @Positive(message = "菜品ID不合法") Long dishId) {
        favoriteService.remove(dishId, UserContext.getUserId());
        return Result.success();
    }
}
