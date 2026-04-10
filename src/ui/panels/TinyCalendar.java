package ui.panels;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

public class TinyCalendar extends JDialog {
    // Ito ang magbabalik ng napiling date
    public boolean dateSelected = false;
    public LocalDate selectedDate = LocalDate.now();
    
    private LocalDate currentMonth = LocalDate.now();
    private JPanel dayPanel;
    private JLabel lblMonthYear;

    public TinyCalendar(Frame parent) {
        super(parent, "Select Date", true);
        setSize(300, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // --- HEADER (Month Navigation) ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        lblMonthYear = new JLabel("", SwingConstants.CENTER);
        lblMonthYear.setFont(new Font("Segoe UI", Font.BOLD, 16));

        btnPrev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); updateCalendar(); });
        btnNext.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); updateCalendar(); });

        header.add(btnPrev, BorderLayout.WEST);
        header.add(lblMonthYear, BorderLayout.CENTER);
        header.add(btnNext, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // --- BODY (Days Grid) ---
        dayPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        dayPanel.setBackground(Color.WHITE);
        dayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(dayPanel, BorderLayout.CENTER);

        updateCalendar();
    }

    private void updateCalendar() {
        dayPanel.removeAll();
        lblMonthYear.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentMonth.getYear());

        // Day Headers (Su, Mo, Tu...)
        String[] days = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (String d : days) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 12));
            l.setForeground(Color.GRAY);
            dayPanel.add(l);
        }

        // Logic to fill empty slots before the 1st day of month
        YearMonth ym = YearMonth.from(currentMonth);
        LocalDate firstOfMonth = currentMonth.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        if (dayOfWeek == 7) dayOfWeek = 0; // Convert Sun to 0 for logic

        for (int i = 0; i < dayOfWeek; i++) {
            dayPanel.add(new JLabel("")); // Empty label
        }

        // Fill days
        int daysInMonth = ym.lengthOfMonth();
        for (int i = 1; i <= daysInMonth; i++) {
            int day = i;
            JButton btn = new JButton(String.valueOf(day));
            btn.setFocusPainted(false);
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
            
            // Highlight Today
            if (day == LocalDate.now().getDayOfMonth() && 
                currentMonth.getMonth() == LocalDate.now().getMonth() &&
                currentMonth.getYear() == LocalDate.now().getYear()) {
                btn.setForeground(new Color(13, 110, 253));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            }

            btn.addActionListener(e -> {
                selectedDate = currentMonth.withDayOfMonth(day);
                dateSelected = true;
                dispose(); // Close dialog
            });
            dayPanel.add(btn);
        }

        dayPanel.revalidate();
        dayPanel.repaint();
    }
}