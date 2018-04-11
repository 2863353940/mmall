package com.mmall.pojo;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor  //无参构造器
@AllArgsConstructor //全参构造器
@EqualsAndHashCode(of = "id")       //重写equals和hashcode方法，只判断id字段
public class Category {
    private Integer id;

    private Integer parentId;

    private String name;

    private Boolean status;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;


    /**
     *  重写Category的equals 和 hashCode方法
     */

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Category category = (Category) o;
//
//        return id != null ? id.equals(category.id) : category.id == null;
//
//    }
//
//    @Override
//    public int hashCode() {
//        return id != null ? id.hashCode() : 0;
//    }
}