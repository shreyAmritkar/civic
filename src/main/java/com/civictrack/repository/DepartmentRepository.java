package com.civictrack.repository;

import com.civictrack.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByActiveTrue();
}
