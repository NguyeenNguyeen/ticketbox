package com.ticketbox.backend.service;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderStatus;
import com.ticketbox.backend.entity.RoleName;
import com.ticketbox.backend.repository.ConcertRepository;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.TicketRepository;
import com.ticketbox.backend.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminStatsService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private UserRepository userRepository;

    public AdminStatsDto getStats() {
        AdminStatsDto stats = new AdminStatsDto();

        // Total revenue
        java.math.BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED);
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue.doubleValue() : 0.0);

        // Tickets sold
        stats.setTicketsSold(ticketRepository.count());

        // Active events (assuming all non-cancelled events for simple metric)
        stats.setActiveEvents(concertRepository.count());

        // Total audience (count of users with CUSTOMER role)
        stats.setTotalAudience(userRepository.countByRole(RoleName.CUSTOMER));

        // 7 days revenue
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0);
        List<Order> recentOrders = orderRepository.findByStatusAndCreatedAtAfter(OrderStatus.COMPLETED, sevenDaysAgo);

        // Group by day string (e.g., "T2", "T3" or "dd/MM")
        Map<String, Double> revenueByDayMap = recentOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> formatDayOfWeek(order.getCreatedAt().toLocalDate()),
                        Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ));

        List<DailyRevenueDto> chartData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dayStr = formatDayOfWeek(date);
            DailyRevenueDto daily = new DailyRevenueDto();
            daily.setDay(dayStr);
            daily.setRevenue(revenueByDayMap.getOrDefault(dayStr, 0.0));
            chartData.add(daily);
        }

        stats.setRevenueChart(chartData);

        return stats;
    }

    private String formatDayOfWeek(LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        if (day == 7) return "CN";
        return "T" + (day + 1);
    }

    @Data
    public static class AdminStatsDto {
        private Double totalRevenue;
        private Long ticketsSold;
        private Long activeEvents;
        private Long totalAudience;
        private List<DailyRevenueDto> revenueChart;
    }

    @Data
    public static class DailyRevenueDto {
        private String day;
        private Double revenue;
    }
}
