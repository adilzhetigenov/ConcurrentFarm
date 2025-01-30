import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.*;

public class Farm {
    private static final int DEFAULT_DIMENSION = 14;
    private static final int SHEEP_COUNT = 10;
    private static final int DOG_COUNT = 5;
    private final Object[][] matrix;
    private final ReentrantLock[][] lockMatrix;
    private final int length;
    private final int width;
    private final int[][] gates = new int[4][2];
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final ExecutorService executorService;
    private final List<Runnable> entities = new ArrayList<>();

    public Farm() {
        this(DEFAULT_DIMENSION, DEFAULT_DIMENSION);
    }

    public Farm(int length, int width) {
        if ((length - 2) % 3 != 0 || (width - 2) % 3 != 0) {
            throw new IllegalArgumentException("Dimensions must be a multiple of three plus two.");
        }
        this.length = length;
        this.width = width;
        this.matrix = new Object[length][width];
        this.lockMatrix = new ReentrantLock[length][width];
        this.executorService = Executors.newFixedThreadPool(SHEEP_COUNT + DOG_COUNT);
        initializeFarm();
    }

    private synchronized void initializeFarm() {
        initializeWallsAndEmptyCells();
        placeGates();
        placeEntities("Sheep", SHEEP_COUNT);
        placeEntities("Dog", DOG_COUNT);
        for (Runnable entity : entities) {
            executorService.execute(entity);
        }
        printFarm();
    }

    private synchronized void initializeWallsAndEmptyCells() {
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < width; j++) {
                if (i == 0 || j == 0 || i == length - 1 || j == width - 1) {
                    matrix[i][j] = new Wall();
                } else {
                    matrix[i][j] = new Empty();
                }
                lockMatrix[i][j] = new ReentrantLock();
            }
        }
    }

    private synchronized void placeGates() {
        for (int i = 0; i < 4; i++) {
            int gatePosition;
            switch (i) {
                case 0:
                    gatePosition = random.nextInt(1, width - 1);
                    gates[i] = new int[]{0, gatePosition};
                    matrix[0][gatePosition] = new Gate();
                    break;
                case 1:
                    gatePosition = random.nextInt(1, width - 1);
                    gates[i] = new int[]{length - 1, gatePosition};
                    matrix[length - 1][gatePosition] = new Gate();
                    break;
                case 2:
                    gatePosition = random.nextInt(1, length - 1);
                    gates[i] = new int[]{gatePosition, 0};
                    matrix[gatePosition][0] = new Gate();
                    break;
                case 3:
                    gatePosition = random.nextInt(1, length - 1);
                    gates[i] = new int[]{gatePosition, width - 1};
                    matrix[gatePosition][width - 1] = new Gate();
                    break;
            }
        }
    }

    private synchronized void placeEntities(String entity, int count) {
        int placed = 0;
        while (placed < count) {
            int x,y;
            if ("Sheep".equals(entity)) {
                int[] xy = randomInnerZonePosition();
                x = xy[0];
                y = xy[1];
                Sheep sheep = new Sheep(this, x, y);
                matrix[x][y] = sheep;
                entities.add(sheep);
            } else if ("Dog".equals(entity)) {
                do {
                    x = random.nextInt(1, width - 1);
                    y = random.nextInt(1, length - 1);
                } while (!isOuterZone(x, y) && !(matrix[x][y] instanceof Empty));
                Dog dog = new Dog(this, x, y);
                matrix[x][y] = dog;
                entities.add(dog);
            }
            placed++;
        }
    }

    public boolean isOuterZone(int x, int y) {
        if (!isWithinBounds(x, y)) return false;
        int innerStartRow = length / 3;
        int innerEndRow = 2 * (length / 3);
        int innerStartCol = width / 3;
        int innerEndCol = 2 * (width / 3);
        return x < innerStartRow || x >= innerEndRow || y < innerStartCol || y >= innerEndCol;
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < length && y >= 0 && y < width;
    }

    private int[] randomInnerZonePosition() {
        int innerStartRow = length / 3;
        int innerEndRow = 2 * (length / 3);
        int innerStartCol = width / 3;
        int innerEndCol = 2 * (width / 3);
        int x, y;
        do {
            x = random.nextInt(innerStartRow, innerEndRow);
            y = random.nextInt(innerStartCol, innerEndCol);
        } while (!(matrix[x][y] instanceof Empty));
        return new int[]{x, y};
    }

    public synchronized void printFarm() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println("\u001B[0;0H");
        for (Object[] row : matrix) {
            for (Object cell : row) {
                System.out.print(cell.toString());
            }
            System.out.println();
        }
    }

    public synchronized boolean isSheepEscaped() {
        for (int[] gate : gates) {
            int gateX = gate[0];
            int gateY = gate[1];
            for (Runnable entity : entities) {
                if (entity instanceof Sheep sheep && sheep.getX() == gateX && sheep.getY() == gateY) {
                        return true;
                }
            }
        }
        return false;
    }

    public synchronized void stopSimulation() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
        }
        System.out.println("Simulation terminated.");
    }

    synchronized Object getEntity(int x, int y) {
        return matrix[x][y];
    }

    synchronized void setEntity(int x, int y, Object entity) {
        matrix[x][y] = entity;
    }

    public ReentrantLock getCellLock(int x, int y) {
        return lockMatrix[x][y];
    }
}

class Empty {
    @Override
    public String toString() {
        return " ";
    }
}

class Wall {
    @Override
    public String toString() {
        return "#";
    }
}

class Gate {
    @Override
    public String toString() {
        return " ";
    }
}
