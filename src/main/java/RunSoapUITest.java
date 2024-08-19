import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class RunSoapUITest {

    // Path to testrunner.bat
    private static final String TESTRUNNER = "C:\\Program Files\\SmartBear\\SoapUI-5.6.1\\bin\\testrunner.bat";

    // Path to SoapUI project file
    private static final String PROJECT_FILE = "C:\\Users\\MohamedBABAOUI\\TNR_BF\\ws_tnr_bf_06_24.xml";

    // Base output directory
    private static final String OUTPUT_DIR_BASE = "C:\\Users\\MohamedBABAOUI\\Reports";

    // Path to the file containing test suite names
    private static final String TEST_SUITE_NAMES_FILE = "C:\\Users\\MohamedBABAOUI\\TestSuitesNames\\TSN.txt";

    private static Map<Integer, String> testSuitesMap = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Run the extract test suite to update TSN.txt
            String extractSuiteName = "ExtractTS";
            System.out.println("Test suites are loading...");
            if (runTestSuite(extractSuiteName, true)) {
                System.out.println("Test suites loaded successfully.");

                // Load and print the test suite names
                loadTestSuites();
                printTestSuites();

                // Display options to the user
                System.out.println("1 - Execute a single test suite");
                System.out.println("2 - Execute a set of test suites");
                System.out.println("3 - Execute all test suites");

                // Get user choice
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter your choice (1, 2, or 3): ");
                int choice = scanner.nextInt();
                scanner.nextLine();  // Consume newline

                // Execute actions based on user choice
                switch (choice) {
                    case 1:
                        // Execute a single test suite
                        System.out.print("Enter the test suite number to execute (1-" + testSuitesMap.size() + "): ");
                        int suiteNumber = scanner.nextInt();
                        scanner.nextLine();  // Consume newline
                        if (testSuitesMap.containsKey(suiteNumber)) {
                            String suiteName = testSuitesMap.get(suiteNumber);
                            runInitTestSuite();  // Run INIT before the selected test suite
                            runSingleSuite(suiteName);
                        } else {
                            System.out.println("Invalid test suite number.");
                        }
                        break;

                    case 2:
                        // Execute a set of test suites
                        System.out.print("Enter the test suite numbers to execute (comma-separated, e.g., 1,3,6): ");
                        String input = scanner.nextLine();
                        String[] indices = input.split(",");
                        runInitTestSuite();  // Run INIT before executing the set of test suites
                        for (String index : indices) {
                            int num = Integer.parseInt(index.trim());
                            if (testSuitesMap.containsKey(num)) {
                                runSingleSuite(testSuitesMap.get(num));
                            } else {
                                System.out.println("Invalid test suite number: " + num);
                            }
                        }
                        break;

                    case 3:
                        // Execute all test suites
                        runInitTestSuite();  // Run INIT before executing all test suites
                        for (String testSuite : testSuitesMap.values()) {
                            runSingleSuite(testSuite);
                        }
                        break;

                    default:
                        System.out.println("Invalid choice.");
                        break;
                }
            } else {
                System.out.println("Failed to run test suite: " + extractSuiteName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to run the INIT test suite
    private static void runInitTestSuite() throws IOException, InterruptedException {
        String initSuiteName = "INIT";
        System.out.println("Running the INIT test suite: " + initSuiteName);
        if (runTestSuite(initSuiteName, false)) {
            System.out.println("Test suite " + initSuiteName + " executed successfully.");
        } else {
            System.out.println("Failed to run test suite: " + initSuiteName);
        }
    }

    // Method to load test suite names from file into a map, excluding "INIT"
    private static void loadTestSuites() throws IOException {
        testSuitesMap.clear();  // Clear any existing entries
        int index = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_SUITE_NAMES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    String name = parts[1];
                    if (!name.equalsIgnoreCase("INIT")) {  // Exclude INIT
                        testSuitesMap.put(index, name);
                        index++;
                    }
                }
            }
        }
    }

    // Method to print test suite names to the console
    private static void printTestSuites() {
        System.out.println("Available Test Suites:");
        for (Map.Entry<Integer, String> entry : testSuitesMap.entrySet()) {
            System.out.println("Test Suite " + entry.getKey() + ": " + entry.getValue());
        }
    }

    // Method to run a test suite and optionally suppress detailed output
    private static boolean runTestSuite(String suiteName, boolean suppressOutput) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "\"" + TESTRUNNER + "\"",
                "-s" + suiteName,
                "-r", "-a", "-j",
                "-f" + OUTPUT_DIR_BASE,
                PROJECT_FILE
        );
        Process process = builder.start();

        if (!suppressOutput) {
            // If not suppressing output, log the output and error streams
            logProcessOutput(process);
        } else {
            // If suppressing output, only log errors if there are any
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            boolean hasError = false;
            while ((line = errorReader.readLine()) != null) {
                hasError = true;
                System.err.println(line);
            }
            if (hasError) {
                return false;  // Return false if there were errors
            }
        }

        return process.waitFor() == 0;  // Return true if the process finished successfully
    }

    // Method to run a single test suite as a subroutine
    private static void runSingleSuite(String suiteName) throws IOException, InterruptedException {
        System.out.println("Running single test suite: " + suiteName);

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "\"" + TESTRUNNER + "\"",
                "-s" + suiteName,
                "-r", "-a", "-j",
                "-f" + OUTPUT_DIR_BASE + "\\" + suiteName,
                PROJECT_FILE
        );
        Process process = builder.start();

        // Capture output and error streams
        logProcessOutput(process);

        if (process.waitFor() == 0) {
            System.out.println("Test suite " + suiteName + " executed successfully.");
        } else {
            System.out.println("Failed to run test suite: " + suiteName);
        }
    }

    // Method to log the output and error streams from a process
    private static void logProcessOutput(Process process) throws IOException {
        // Reading the standard output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        System.out.println("Standard Output:");
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // Reading the error stream
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        System.out.println("Error Output (if any):");
        while ((line = errorReader.readLine()) != null) {
            System.err.println(line);
        }
    }
}
