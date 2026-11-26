package Ultra;

import branches.*;
import model.Branch;
import InputAndProcess.RadioStationSimulator;

import java.util.Scanner;

/**
 * Application entry point for the ULTRA (Bletchley Park Intelligence System) simulation.
 * <p>
 * Initializes the central {@link Ultra} orchestrator, registers all five military branch
 * observers ({@link Army}, {@link Navy}, {@link RAF}, {@link PMOFFICE}, and {@link SHAEF}),
 * and provides an interactive command-line menu for simulating radio message interception
 * and triggering the decryption and intelligence routing pipeline.
 */
public class Main {

    /**
     * Launches the interactive CLI menu loop.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        // Set up the Ultra orchestrator with all branch observers
        Ultra ultra = new Ultra();
        ultra.registerObserver(Branch.ARMY, new Army());
        ultra.registerObserver(Branch.NAVY, new Navy());
        ultra.registerObserver(Branch.RAF, new RAF());
        ultra.registerObserver(Branch.PM_OFFICE, new PMOFFICE());
        ultra.registerObserver(Branch.SHAEF, new SHAEF());

        Scanner sc = new Scanner(System.in);

        System.out.println("=== ULTRA - Bletchley Park Intelligence System ===");
        System.out.println("1. Insert intercepted messages (Radio Simulator)");
        System.out.println("2. Process pending messages");
        System.out.println("3. Insert then process");
        System.out.println("4. Exit");

        while (true) {
            System.out.print("\nSelect option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    // Launch interactive radio interception simulator
                    new RadioStationSimulator().receive();
                    break;

                case "2":
                    // Process a batch of pending messages from the database
                    ultra.processMessages();
                    break;

                case "3":
                    // Intercept new messages and immediately run the processing pipeline
                    new RadioStationSimulator().receive();
                    System.out.println("\n--- Now processing intercepted messages ---\n");
                    ultra.processMessages();
                    break;

                case "4":
                    System.out.println("[Ultra] System shutting down.");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid option. Try 1-4.");
            }
        }
    }
}
