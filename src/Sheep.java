import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class Sheep implements Runnable {
    private final Farm farm;
    private int x, y;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Sheep(Farm farm, int x, int y) {
        this.farm = farm;
        this.x = x;
        this.y = y;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (farm) {
                moveSheep();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void moveSheep() {
        ReentrantLock currentCellLock = farm.getCellLock(x, y);
        int dx = random.nextInt(-1, 2);
        int dy = random.nextInt(-1, 2);

        while ((dx == 0 && dy == 0) || !(farm.isWithinBounds(x + dx, y + dy)) || !(farm.getEntity(x + dx, y + dy) instanceof Empty || farm.getEntity(x + dx, y + dy) instanceof Gate)) {
            dx = random.nextInt(-1, 2);
            dy = random.nextInt(-1, 2);
        }

        ReentrantLock newCellLock = farm.getCellLock(x + dx, y + dy);

        if (currentCellLock != newCellLock) {
            currentCellLock.lock();
            newCellLock.lock();
        }

        try {
            farm.setEntity(x, y, new Empty());
            x += dx;
            y += dy;
            farm.setEntity(x, y, this);
        } finally {
            if (currentCellLock != newCellLock) {
                currentCellLock.unlock();
                newCellLock.unlock();
            }
        }
    }

    @Override
    public String toString() {
        return "S";
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
