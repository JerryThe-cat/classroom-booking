package com.example.classroombooking.utils;

import com.example.classroombooking.service.BookingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingScheduler {
    private final BookingService bookingService;

    public BookingScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Shanghai")
    public void releaseExpiredBookings() {
        bookingService.autoReleaseExpired();
    }
}
