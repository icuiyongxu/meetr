package com.meetr.domain.repository;

import com.meetr.domain.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findAllByOrderBySortNoAscIdAsc();
}
