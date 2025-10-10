package Part_Service;

import java.io.*;
import java.util.*;

public class TaxCalculation {

    public static void calculateTax(String userId, String year) {
        File inputFile = new File("user_data.csv");
        File outputFile = new File("taxhistory.csv");

        if (!inputFile.exists()) {
            System.err.println("ไม่พบไฟล์ user_data.csv");
            return;
        }

        List<String> history = new ArrayList<>();
        if (outputFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
                String l;
                while ((l = br.readLine()) != null) history.add(l);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (history.isEmpty()) {
            history.add("UserID,Year,TotalIncome,TotalDeduction,NetIncome,Tax");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",", -1);
                if (data.length < 17) continue;
                if (!data[0].equals(userId) || !data[1].equals(year)) continue;

                // ===== (ช่อง3 + ช่อง5)*12 + ช่อง4 =====
                double monthlySalary = parseDouble(data[2]);
                double otherMonthly = parseDouble(data[3]);
                double bonus = parseDouble(data[4]);
                double totalIncome = (monthlySalary + otherMonthly) * 12 + bonus;

                // ===== ช่อง6ถึงช่อง16 (ค่าลดหย่อน) =====
                double totalDeduction = 0;
                for (int i = 6; i <= 16 && i < data.length; i++) {
                    totalDeduction += parseDouble(data[i]);
                }

                // ===== รายได้สุทธิ =====
                double netIncome = Math.max(totalIncome - totalDeduction, 0);

                // ===== คำนวณภาษีขั้นบันได =====
                double tax = calculateProgressiveTax(netIncome);

                // ===== เขียนผลลัพธ์ =====
                history.removeIf(h -> h.startsWith(userId + "," + year + ","));
                history.add(userId + "," + year + "," +
                        format(totalIncome) + "," +
                        format(totalDeduction) + "," +
                        format(netIncome) + "," +
                        format(tax));
            }

            try (FileWriter fw = new FileWriter(outputFile, false)) {
                for (String l : history) fw.write(l + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== ภาษีขั้นบันได =====
    private static double calculateProgressiveTax(double income) {
        if (income <= 150000) return 0;

        double[][] brackets = {
            {150000, 0.00},
            {300000, 0.05},
            {500000, 0.10},
            {750000, 0.15},
            {1000000, 0.20},
            {2000000, 0.25},
            {5000000, 0.30},
            {Double.MAX_VALUE, 0.35}
        };

        double tax = 0;
        double lastLimit = 0;

        for (double[] b : brackets) {
            double limit = b[0];
            double rate = b[1];
            if (income > limit) {
                tax += (limit - lastLimit) * rate;
                lastLimit = limit;
            } else {
                tax += (income - lastLimit) * rate;
                break;
            }
        }

        return tax;
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim().replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String format(double n) {
        if (Math.abs(n - Math.round(n)) < 0.001)
            return String.valueOf((long) Math.round(n));
        else
            return String.format("%.2f", n);
    }
}
