package com.grs.core.repo;

import com.grs.core.domain.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Created by Acer on 9/20/2017.
 */
@NoRepositoryBean
public interface BaseEntityRepo<T extends BaseEntity> extends JpaRepository<T, Long> {
}
