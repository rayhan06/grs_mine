package com.grs.core.repo.projapoti;

import com.grs.core.domain.projapoti.CustomOfficeLayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomOfficeLayerRepo extends JpaRepository<CustomOfficeLayer, Long> {
    List<CustomOfficeLayer> findByLayerLevel(Integer layerLevel);
}
