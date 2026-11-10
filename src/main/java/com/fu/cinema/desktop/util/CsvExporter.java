package com.fu.cinema.desktop.util;

import com.fu.cinema.desktop.model.ReportBookingRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvExporter() {}

    public static void writeBookings(Path destination, List<ReportBookingRow> bookings) throws IOException {
        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".report-", ".csv");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write('\uFEFF');
                writer.write("Mã đơn,Ngày đặt,Khách hàng,Phim,Số vé,Tổng tiền (VND)\r\n");
                for (ReportBookingRow booking : bookings) {
                    writer.write(String.join(",", Long.toString(booking.id()), DATE.format(booking.bookingDate()),
                            quoteText(booking.customerName()), quoteText(booking.movieTitle()),
                            Integer.toString(booking.ticketCount()), booking.totalAmount().toPlainString()));
                    writer.write("\r\n");
                }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String quoteText(String value) {
        String trimmed = value.stripLeading();
        // Excel can interpret quoted text as a formula when a cell begins with an operator.
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
