package com.upendra.dispatch.repository;

import com.upendra.dispatch.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Summary report built with SQL CTEs (WITH clauses).
     * Returns exactly one row per priority (HIGH, MEDIUM, LOW), even when a
     * priority has no orders, plus fleet totals on every row.
     *
     * Columns: priority, order_count, total_weight, vehicle_count, total_capacity
     */
    @Query(value = """
            WITH priorities AS (
                SELECT 'HIGH' AS priority, 1 AS rank_no
                UNION ALL SELECT 'MEDIUM', 2
                UNION ALL SELECT 'LOW', 3
            ),
            order_stats AS (
                SELECT priority,
                       COUNT(*)            AS order_count,
                       SUM(package_weight) AS total_weight
                FROM orders
                GROUP BY priority
            ),
            fleet AS (
                SELECT COUNT(*)                   AS vehicle_count,
                       COALESCE(SUM(capacity), 0) AS total_capacity
                FROM vehicles
            )
            SELECT p.priority,
                   COALESCE(s.order_count, 0)  AS order_count,
                   COALESCE(s.total_weight, 0) AS total_weight,
                   f.vehicle_count,
                   f.total_capacity
            FROM priorities p
            LEFT JOIN order_stats s ON s.priority = p.priority
            CROSS JOIN fleet f
            ORDER BY p.rank_no
            """, nativeQuery = true)
    List<Object[]> fetchDispatchSummaryRows();
}
