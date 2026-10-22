import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

/**
 * Academic Student Performance & At-Risk Predictor
 *
 * CSV format:
 * id,name,attendance,assessment,studyHours
 *
 * Example:
 * S001,Asha Patel,72,65,4
 */
public class AcademicStudentPerformancePredictor {
    private static final double STUDY_HOURS_TARGET = 10.0;
    private static final double MEDIUM_RISK_LIMIT = 40.0;
    private static final double HIGH_RISK_LIMIT = 70.0;

    // Provides average O(1) student lookup by ID.
    private final Map<String, Student> students = new HashMap<>();

    // Highest risk score is placed at the front of this priority queue.
    private final PriorityQueue<Student> riskQueue = new PriorityQueue<>(
        Comparator.comparingDouble(Student::getRiskScore)
            .reversed()
            .thenComparing(Student::getId)
    );

    private static class Student {
        private final String id;
        private final String name;
        private final double attendance;
        private final double assessment;
        private final double studyHours;
        private final double riskScore;

        Student(String id, String name, double attendance,
                double assessment, double studyHours) {
            this.id = id;
            this.name = name;
            this.attendance = attendance;
            this.assessment = assessment;
            this.studyHours = studyHours;
            this.riskScore = calculateRisk(attendance, assessment, studyHours);
        }

        String getId() {
            return id;
        }

        double getRiskScore() {
            return riskScore;
        }

        String getRiskLevel() {
            if (riskScore >= HIGH_RISK_LIMIT) {
                return "HIGH";
            } else if (riskScore >= MEDIUM_RISK_LIMIT) {
                return "MEDIUM";
            }
            return "LOW";
        }
    }

    /*
     * Assumed weighted risk formula:
     * 40% attendance deficit, 40% assessment deficit,
     * and 20% study-hours deficit relative to 10 hours/week.
     */
    private static double calculateRisk(
            double attendance, double assessment, double studyHours) {

        double attendanceRisk = 100.0 - attendance;
        double assessmentRisk = 100.0 - assessment;
        double studyRisk = Math.max(
            0.0,
            (STUDY_HOURS_TARGET - studyHours) / STUDY_HOURS_TARGET * 100.0
        );

        double score = 0.40 * attendanceRisk
                     + 0.40 * assessmentRisk
                     + 0.20 * studyRisk;

        return Math.max(0.0, Math.min(100.0, score));
    }

    private void loadCsv(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String header = reader.readLine();

            if (header == null) {
                throw new IOException("The CSV file is empty.");
            }

            Map<String, Integer> columns = parseHeader(header);
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    List<String> fields = parseCsvLine(line);

                    String id = getValue(fields, columns, "id");
                    String name = getValue(fields, columns, "name");
                    double attendance = getNumber(fields, columns, "attendance");
                    double assessment = getNumber(fields, columns, "assessment");
                    double studyHours = getNumber(fields, columns, "studyhours");

                    if (id.isEmpty() || name.isEmpty()) {
                        throw new IllegalArgumentException(
                            "Student ID and name are required."
                        );
                    }

                    if (attendance < 0 || attendance > 100
                            || assessment < 0 || assessment > 100
                            || studyHours < 0) {
                        throw new IllegalArgumentException(
                            "Attendance and assessment must be 0-100; "
                            + "study hours must be non-negative."
                        );
                    }

                    Student student = new Student(
                        id, name, attendance, assessment, studyHours
                    );

                    students.put(id, student);
                } catch (RuntimeException e) {
                    System.err.println(
                        "Skipping line " + lineNumber + ": " + e.getMessage()
                    );
                }
            }
        }

        riskQueue.clear();
        riskQueue.addAll(students.values());
    }

    private static Map<String, Integer> parseHeader(String header) {
        List<String> names = parseCsvLine(header);
        Map<String, Integer> columns = new HashMap<>();

        for (int i = 0; i < names.size(); i++) {
            columns.put(names.get(i).trim().toLowerCase(), i);
        }

        String[] required = {
            "id", "name", "attendance", "assessment", "studyhours"
        };

        for (String column : required) {
            if (!columns.containsKey(column)) {
                throw new IllegalArgumentException(
                    "Missing CSV column: " + column
                );
            }
        }

        return columns;
    }

    // Handles quoted values and escaped quote characters in CSV fields.
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (character == '"') {
                if (insideQuotes && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ',' && !insideQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        if (insideQuotes) {
            throw new IllegalArgumentException(
                "Unclosed quoted value in CSV row."
            );
        }

        fields.add(current.toString().trim());
        return fields;
    }

    private static String getValue(
            List<String> fields, Map<String, Integer> columns, String column) {

        int index = columns.get(column);

        if (index >= fields.size()) {
            throw new IllegalArgumentException(
                "Missing value for " + column
            );
        }

        return fields.get(index).trim();
    }

    private static double getNumber(
            List<String> fields, Map<String, Integer> columns, String column) {

        return Double.parseDouble(getValue(fields, columns, column));
    }

    private void showDashboard() {
        long highRisk = students.values().stream()
            .filter(student -> student.getRiskLevel().equals("HIGH"))
            .count();

        long mediumRisk = students.values().stream()
            .filter(student -> student.getRiskLevel().equals("MEDIUM"))
            .count();

        long lowRisk = students.size() - highRisk - mediumRisk;

        System.out.println("\n=== Academic Risk Dashboard ===");
        System.out.println("Students loaded: " + students.size());
        System.out.println("High risk: " + highRisk);
        System.out.println("Medium risk: " + mediumRisk);
        System.out.println("Low risk: " + lowRisk);

        System.out.println("\nStudents with the highest risk:");

        if (riskQueue.isEmpty()) {
            System.out.println("No student records are available.");
            return;
        }

        riskQueue.stream()
            .limit(10)
            .forEach(AcademicStudentPerformancePredictor::printStudent);

        if (highRisk > 0) {
            System.out.println(
                "\nALERT: " + highRisk
                + " student(s) need immediate advisor attention."
            );
        } else {
            System.out.println(
                "\nNo students currently meet the high-risk threshold."
            );
        }
    }

    private void findStudent(Scanner scanner) {
        System.out.print("Enter student ID: ");
        String id = scanner.nextLine().trim();

        Student student = students.get(id);

        if (student == null) {
            System.out.println("Student not found.");
        } else {
            printStudent(student);
        }
    }

    private void printReport() {
        System.out.println("\n=== Academic Performance Summary ===");
        System.out.printf(
            "%-12s %-22s %12s %12s %12s %10s %10s%n",
            "ID", "Name", "Attendance", "Assessment",
            "Study Hours", "Risk", "Level"
        );

        students.values().stream()
            .sorted(Comparator.comparing(Student::getId))
            .forEach(student -> System.out.printf(
                "%-12s %-22s %11.1f%% %11.1f%% %12.1f %9.1f%% %10s%n",
                student.id,
                student.name,
                student.attendance,
                student.assessment,
                student.studyHours,
                student.riskScore,
                student.getRiskLevel()
            ));
    }

    private static void printStudent(Student student) {
        System.out.printf(
            "ID: %s | Name: %s | Attendance: %.1f%% | "
            + "Assessment: %.1f%% | Study hours/week: %.1f | "
            + "Risk: %.1f%% (%s)%n",
            student.id,
            student.name,
            student.attendance,
            student.assessment,
            student.studyHours,
            student.riskScore,
            student.getRiskLevel()
        );
    }

    private void runMenu() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n1. Dashboard and alerts");
                System.out.println("2. Find student by ID");
                System.out.println("3. Performance summary report");
                System.out.println("0. Exit");
                System.out.print("Select an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        showDashboard();
                        break;
                    case "2":
                        findStudent(scanner);
                        break;
                    case "3":
                        printReport();
                        break;
                    case "0":
                        System.out.println("Goodbye.");
                        return;
                    default:
                        System.out.println("Please enter 1, 2, 3, or 0.");
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(
                "Usage: java AcademicStudentPerformancePredictor <students.csv>"
            );
            System.out.println(
                "CSV columns: id,name,attendance,assessment,studyHours"
            );
            return;
        }

        AcademicStudentPerformancePredictor app =
            new AcademicStudentPerformancePredictor();

        try {
            app.loadCsv(Path.of(args[0]));
            app.runMenu();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}