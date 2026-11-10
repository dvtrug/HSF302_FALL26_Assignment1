package com.fu.cinema.desktop;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.util.CsvExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvExporterTest {

    @TempDir Path output;

    @Test
    void writesBomVietnameseCsvEscapingAndNumericAmounts() throws IOException {
        Path file = output.resolve("doanh-thu.csv");
        Files.writeString(file, "Nội dung cũ");
        var booking = new ReportBookingRow(12, LocalDateTime.of(2026, 11, 9, 19, 30),
                "Nguyễn \"An\", Jr.", "Cánh đồng\nMùa hạ", 2, new BigDecimal("160000.00"));
        var formula = new ReportBookingRow(13, booking.bookingDate(), "=1+1", "Phim", 1, BigDecimal.TEN);
        CsvExporter.writeBookings(file, List.of(booking, formula));

        assertEquals("\uFEFFMã đơn,Ngày đặt,Khách hàng,Phim,Số vé,Tổng tiền (VND)\r\n"
                + "12,2026-11-09 19:30:00,\"Nguyễn \"\"An\"\", Jr.\",\"Cánh đồng\nMùa hạ\",2,160000.00\r\n"
                + "13,2026-11-09 19:30:00,\"'=1+1\",\"Phim\",1,10\r\n",
                Files.readString(file, StandardCharsets.UTF_8));
    }
}
