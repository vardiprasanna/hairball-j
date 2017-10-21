package com.oath.gemini.merchant.db;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Where;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@Getter
@Setter
@MappedSuperclass
@Where(clause="is_deleted = 0")
public abstract class StoreBaseEntity {
    public StoreBaseEntity() {
        updatedDate = createdDate = new Timestamp(System.currentTimeMillis());
    }

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "cr_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Timestamp createdDate;

    @Column(name = "upd_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Timestamp updatedDate;
}
