package model;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> p = new PageResult<>();
        p.setRecords(records);
        p.setTotal(total);
        p.setPage(page);
        p.setSize(size);
        return p;
    }
}
