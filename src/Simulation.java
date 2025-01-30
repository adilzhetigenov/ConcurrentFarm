public class Simulation {
    public static void main(String[] args) {
        Farm farm = new Farm();

        Thread simulationThread = new Thread(() -> {
            while (!farm.isSheepEscaped() && !Thread.currentThread().isInterrupted()) {
                synchronized (farm) {
                    farm.printFarm();
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        simulationThread.start();

        try {
            simulationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (farm) {
            if (farm.isSheepEscaped()) {
                farm.printFarm();
                System.out.println("A sheep has escaped!\n Please wait a few seconds...");
            }
            farm.stopSimulation();
        }
    }
}
