package com.qmz.canteen.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Data
public class PageResult<T> {

    private List<T> records;
    private Long total;
    private Long current;
    private Long size;
    private Long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords() == null ? Collections.emptyList() : page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        PageResult<T> result = new PageResult<>();
        if (page.getRecords() == null) {
            result.setRecords(Collections.emptyList());
        } else {
            result.setRecords(page.getRecords().stream().map(converter).toList());
        }
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    public static <T> PageResult<T> empty(long current, long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0L);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages(0L);
        return result;
    }
}
